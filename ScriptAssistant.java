import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.nio.file.*;

public class ScriptAssistant {
	static JFrame frame;
	static DefaultListModel<String> data;
	static Clipboard clipboard;
	static final Font font = new Font("Consolas", Font.PLAIN, 17);
    static String UserDir = System.getProperty("user.home");
    static String AppDir = "/AppData/Roaming/ScriptAssistant";
    static String SettingsPath = "settings.dat";
    static String ScriptPath = "script.dat";
    static String RecordDelimiter = "\30"; // ascii record delimiter
    static int left = 100;
    static int top = 100;
    static int width = 800;
    static int height = 600;
	static int usingIndex = -1;

    private static class NoSelectionModel extends DefaultListSelectionModel {
        @Override
        public void setAnchorSelectionIndex(final int anchorIndex) {}

        @Override
        public void setLeadAnchorNotificationEnabled(final boolean flag) {}

        @Override
        public void setLeadSelectionIndex(final int leadIndex) {}

        @Override
        public void setSelectionInterval(final int index0, final int index1) { }
    }

    private static ListCellRenderer<? super String> getRenderer() {
        return new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                    Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
				if(value.equals(""))
						value = "&nbsp;";
				value = "<html><pre style='margin: 0px; padding: 0px 0px 2px 0px; font-family: Consolas;font-size: 17;font-weight: normal;'>" + value + "</pre></html>";
				JLabel listCellRendererComponent = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,cellHasFocus);
				listCellRendererComponent.setFont(font);
                if(index < list.getModel().getSize() - 1)
                    listCellRendererComponent.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,Color.LIGHT_GRAY));
                return listCellRendererComponent;
            }
        };
    }

    private static String ReadFile(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(UserDir, AppDir, path)));
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean SaveFile(String path, String data) {
        try {
            Files.write(Paths.get(UserDir, AppDir, path), data.getBytes());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String TextAreaDialog(String defaultStr, String title) {
		JTextArea msg = new JTextArea(defaultStr);
		msg.setLineWrap(false);
		msg.setFont(font);
		JScrollPane scrollPane = new JScrollPane(msg); 
		scrollPane.setPreferredSize(new Dimension(800, 600));

        if(JOptionPane.showConfirmDialog(frame, scrollPane, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            return msg.getText();
        }

        return defaultStr;
	}

	private static void FixBlanks() {
		int c = data.getSize() - 1;
		if(c < 0 || data.getElementAt(c).length() > 0) {
			data.addElement("");
		} else {
			c--;
			while(c >= 0 && data.getElementAt(c).length() < 1) {
				data.removeElementAt(c);
				c--;
			}
		}
	}

	private static void MoveIndex(int start, int end) {
		String tmp = data.getElementAt(start);
		data.removeElementAt(start);
		data.add(end, tmp);
		FixBlanks();
	}

    public static void main(String[] args) {
        File dir = new File(Paths.get(UserDir, AppDir).toString());
        if(!dir.exists())
			dir.mkdir();

        String settingsstr = ReadFile(SettingsPath);
        String[] settings = settingsstr.split(RecordDelimiter);

        if(settings.length >= 4) {
            left = Integer.parseInt(settings[0]);
            top = Integer.parseInt(settings[1]);
            width = Integer.parseInt(settings[2]);
            height = Integer.parseInt(settings[3]);
        }

        clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		frame = new JFrame("Script Assistant");
		frame.setFont(font);
        data = new DefaultListModel<String>();

		JList<String> datalist = new JList<String>(data);
		datalist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane datascroll = new JScrollPane(datalist); 
        frame.add(datascroll);
        frame.setLocationRelativeTo(null);
        frame.setBounds(left, top, width, height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //datalist.setSelectionModel(new NoSelectionModel());
		datalist.setCellRenderer(getRenderer());
		String[] scriptEntries = ReadFile(ScriptPath).split(RecordDelimiter);
		for(int c = 0; c < scriptEntries.length; c++)
			data.addElement(scriptEntries[c]);
		FixBlanks();

		datalist.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				int i = datalist.getSelectedIndex();
				String ret;
				if(i < 0)
					return;
				switch(e.getKeyCode()) {
				case 155:
					ret = TextAreaDialog("", "Insert a message!");
					if(!ret.equals("")) {
						data.add(i, ret);
						datalist.setSelectedIndex(i);
					}
					break;
				case 127:
					clipboard.setContents(new StringSelection(data.getElementAt(i)), null);
					data.remove(i);
                    FixBlanks();
					break;
				}
			}
		});

        datalist.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
				int i = datalist.locationToIndex(e.getPoint());
				if(i < 0)
					return;
                switch(e.getButton()) {
                case MouseEvent.BUTTON1:
                    String tmp = data.getElementAt(i);
                    if(tmp.length() > 0)
                        clipboard.setContents(new StringSelection(tmp), null);
                    break;
                case MouseEvent.BUTTON3:
                    String ret = TextAreaDialog(data.getElementAt(i), i < data.getSize() - 1 ? "Edit the message!" : "Insert a message!");
                    if(!ret.equals(data.getElementAt(i))) {
                        data.set(i, ret);
					}
                    FixBlanks();
                    break;
                }
			}
			
			public void mousePressed(MouseEvent e) {
				if(e.getButton() != MouseEvent.BUTTON1)
					return;
				usingIndex = datalist.locationToIndex(e.getPoint());
			}

			public void mouseReleased(MouseEvent e) {
				if(e.getButton() != MouseEvent.BUTTON1)
					return;
				int endIndex = datalist.locationToIndex(e.getPoint());
				if(usingIndex >= 0 && endIndex != usingIndex) {
					MoveIndex(usingIndex, endIndex);
					usingIndex = endIndex;
				}
				usingIndex = -1;
			}
		});
		
		datalist.addMouseMotionListener(new MouseAdapter() {
			public void mouseDragged(MouseEvent e) {
				int endIndex = datalist.locationToIndex(e.getPoint());
				if(usingIndex >= 0 && endIndex != usingIndex) {
					MoveIndex(usingIndex, endIndex);
					usingIndex = endIndex;
				}
			}
		});

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                String savestr =  left + RecordDelimiter + top + RecordDelimiter + width + RecordDelimiter + height;
				SaveFile(SettingsPath, savestr);
				for(int c = 0; c < data.getSize(); c++) {
					if(c < 1) {
						savestr = data.getElementAt(c);
					} else {
						savestr = savestr + RecordDelimiter + data.getElementAt(c);
					}
				}
				SaveFile(ScriptPath, savestr);
            }
        });

        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                width = frame.getWidth();
                height = frame.getHeight();
            }

            public void componentMoved(ComponentEvent e) {
                left = frame.getX();
                top = frame.getY();
            }
        });

        frame.setVisible(true);
    }
}
