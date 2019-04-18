import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.lang.*;

public class ScriptAssistant extends JFrame {
	JFrame frame;
	DefaultListModel<String> data;
	JList<String> datalist;
	Clipboard clipboard;
	final String fontName = "Consolas";
	final int fontSize = 12;
	final Font font = new Font(fontName, Font.PLAIN, fontSize);
	String UserDir = System.getProperty("user.home");
	String AppDir = "/AppData/Roaming/ScriptAssistant";
	String SettingsPath = "settings.dat";
	String ScriptPath = "script.dat";
	String userName = null;
	String RecordDelimiter = "\30"; // ascii record delimiter
	int left = 100;
	int top = 100;
	int width = 800;
	int height = 600;
	int usingIndex = -1;
	boolean hasEditedSettings = false;
	boolean hasEditedScript = false;
	boolean handCursor = false;

	private class ScriptAssistantMouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			int i = datalist.locationToIndex(e.getPoint());
			if (i < 0)
				return;
			switch (e.getButton()) {
			case MouseEvent.BUTTON1:
				String tmp = data.getElementAt(i).replace("%name%", userName).replace("%NAME%", userName.toUpperCase());
				URI url;
				if (tmp.length() > 0) {
					if(tmp.charAt(0) == '@') {
						try {
							url = new URI(tmp.substring(1));
							java.awt.Desktop.getDesktop().browse(url);
						} catch (Exception err) {
							// just ignore it, I guess
						}
					} else {
						clipboard.setContents(new StringSelection(tmp), null);
					}
				}
				break;
			case MouseEvent.BUTTON3:
				String ret = TextAreaDialog(data.getElementAt(i),
						i < data.getSize() - 1 ? "Edit the message!" : "Insert a message!");
				if (!ret.equals(data.getElementAt(i))) {
					data.set(i, ret);
					hasEditedScript = true;
				}
				FixBlanks();
				break;
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if (e.getButton() != MouseEvent.BUTTON1)
				return;
			usingIndex = datalist.locationToIndex(e.getPoint());
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (e.getButton() != MouseEvent.BUTTON1)
				return;
			int endIndex = datalist.locationToIndex(e.getPoint());
			if (usingIndex >= 0 && endIndex != usingIndex) {
				MoveIndex(usingIndex, endIndex);
				usingIndex = endIndex;
			}
			usingIndex = -1;
		}

		public void updateCursor(int index) {
			if(index < 0)
				return;
			String tmp = data.getElementAt(index);
			if(tmp.length() > 0 && tmp.charAt(0) == '@') {
				if(!handCursor) {
					datalist.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
					handCursor = true;
				}
			} else if(handCursor) {
				datalist.setCursor(Cursor.getDefaultCursor());
				handCursor = false;
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			updateCursor(datalist.locationToIndex(e.getPoint()));
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			int endIndex = datalist.locationToIndex(e.getPoint());
			if (usingIndex >= 0 && endIndex != usingIndex) {
				MoveIndex(usingIndex, endIndex);
				usingIndex = endIndex;
			}

			updateCursor(endIndex);
		}
	}

	private class ScriptAssistantKeyListener extends KeyAdapter {
		@Override
		public void keyReleased(KeyEvent e) {
			int i = datalist.getSelectedIndex();
			String ret;
			if (i < 0)
				return;
			switch (e.getKeyCode()) {
			case 155:
				ret = TextAreaDialog("", "Insert a message!");
				if (!ret.equals("")) {
					data.add(i, ret);
					datalist.setSelectedIndex(i);
					hasEditedScript = true;
				}
				break;
			case 127:
				clipboard.setContents(new StringSelection(data.getElementAt(i)), null);
				data.remove(i);
				FixBlanks();
				hasEditedScript = true;
				break;
			}
		}
	}

	private class ScriptAssistantComponentListener extends ComponentAdapter {
		@Override
		public void componentResized(ComponentEvent e) {
			if (width != frame.getWidth() || height != frame.getHeight()) {
				width = frame.getWidth();
				height = frame.getHeight();
				hasEditedSettings = true;
			}
		}

		@Override
		public void componentMoved(ComponentEvent e) {
			if (left != frame.getX() || top != frame.getY()) {
				left = frame.getX();
				top = frame.getY();
				hasEditedSettings = true;
			}
		}
	}

	private class ScriptAssistantWindowListener extends WindowAdapter {
		@Override
		public void windowClosing(WindowEvent e) {
			String savestr = "";

			if (hasEditedSettings) {
				savestr = left + RecordDelimiter + top + RecordDelimiter + width + RecordDelimiter + height;

				if (userName != null) {
					savestr = savestr + RecordDelimiter + userName;
				}

				SaveFile(SettingsPath, savestr);
			}

			if (hasEditedScript) {
				for (int c = 0; c < data.getSize(); c++) {
					if (c < 1) {
						savestr = data.getElementAt(c);
					} else {
						savestr = savestr + RecordDelimiter + data.getElementAt(c);
					}
				}
				SaveFile(ScriptPath, savestr);
			}
		}
	}

	private class ScriptAssistantListCellRenderer extends DefaultListCellRenderer {
		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			String tmp = value.toString();
			if (tmp.equals(""))
				tmp = "&nbsp;";

			if (tmp.charAt(0) == '@' && tmp.length() > 1) {
				tmp = "<html><a href=" + tmp.substring(1) + " style='margin: 0px; padding: 0px 0px 2px 0px; font-family: " + fontName
				+ ";font-size: " + fontSize + ";'>" + tmp.substring(1) + "</a></html>";
			} else {
				tmp = tmp.replace("<", "&lt;").replace(">", "&gt;")
				.replace("[b]", "<span style='font-weight: bold;'>").replace("[/b]", "</span>")
				.replace("[u]", "<span style='text-decoration: underline;'>").replace("[/u]", "</span>")
				.replace("[i]", "<span style='font-style: italic;'>").replace("[/i]", "</span>")
				.replace("[h]", "<span style='font-size: 1.2em;font-weight: bold;text-decoration: underline;'>").replace("[/h]", "</span>");
				
				if (!userName.equals("")) {
					tmp = tmp.replace("%name%", userName).replace("%NAME%", userName.toUpperCase());
				}
		
				tmp = "<html><pre style='margin: 0px; padding: 0px 0px 2px 0px; font-family: " + fontName
						+ ";font-size: " + fontSize + ";font-weight: normal;'>" + tmp + "</pre></html>";
			}
			value = tmp;
			JLabel listCellRendererComponent = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (index < list.getModel().getSize() - 1)
				listCellRendererComponent.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
			return listCellRendererComponent;
		}
	}

	private String ReadFile(String path) {
		try {
			return new String(Files.readAllBytes(Paths.get(UserDir, AppDir, path)));
		} catch (Exception e) {
			// fail, let's try something else
		}
		try {
			InputStream is = getClass().getResourceAsStream(path);
			StringBuilder str = new StringBuilder();
			String line = null;
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));	
			while ((line = bufferedReader.readLine()) != null) {
				str.append(line);
				str.append("\n");
			}		
			return str.toString();
		} catch (Exception e) {
			return "";
		}
	}

	private boolean SaveFile(String path, String data) {
		try {
			Files.write(Paths.get(UserDir, AppDir, path), data.getBytes());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private String TextAreaDialog(String defaultStr, String title) {
		JTextArea msg = new JTextArea(userName.equals("") ? defaultStr : defaultStr.replace("%name%", userName).replace("%NAME%", userName.toUpperCase()));
		msg.setLineWrap(false);
		msg.setFont(font);
		JScrollPane scrollPane = new JScrollPane(msg);
		scrollPane.setPreferredSize(new Dimension(800, 600));

		if (JOptionPane.showConfirmDialog(frame, scrollPane, title,
				JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
			return userName.equals("") ? msg.getText() : msg.getText().replace(userName, "%name%").replace(userName.toUpperCase(), "%NAME%");
		}

		return defaultStr;
	}

	private void FixBlanks() {
		int c = data.getSize() - 1;
		if (c < 0 || data.getElementAt(c).length() > 0) {
			data.addElement("");
		} else {
			c--;
			while (c >= 0 && data.getElementAt(c).length() < 1) {
				data.removeElementAt(c);
				c--;
			}
		}
	}

	private void MoveIndex(int start, int end) {
		String tmp = data.getElementAt(start);
		data.removeElementAt(start);
		data.add(end, tmp);
		FixBlanks();
		hasEditedScript = true;
	}

	ScriptAssistant(String title) {
		super(title);
		frame = this;
		File dir = new File(Paths.get(UserDir, AppDir).toString());
		if (!dir.exists())
			dir.mkdir();

		String settingsstr = ReadFile(SettingsPath);
		String[] settings = settingsstr.split(RecordDelimiter);

		try {
			left = Integer.parseInt(settings[0]);
			top = Integer.parseInt(settings[1]);
			width = Integer.parseInt(settings[2]);
			height = Integer.parseInt(settings[3]);
			userName = settings[4];
		} catch (Exception e) {
			// passively fail; use defaults for anything not loaded
		}

		clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		frame.setFont(font);
		data = new DefaultListModel<String>();

		datalist = new JList<String>(data);
		datalist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane datascroll = new JScrollPane(datalist);
		frame.add(datascroll);
		frame.setLocationRelativeTo(null);
		frame.setBounds(left, top, width, height);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		datalist.setCellRenderer(new ScriptAssistantListCellRenderer());

		String scriptEntriesData = ReadFile(ScriptPath);

		if (userName == null || userName.equals("")) {
			userName = JOptionPane.showInputDialog("Enter your name as it would appear in ticket notes:");
			if (userName != null && !userName.equals("")) {
				hasEditedSettings = true;
			}
		}

		if (userName == null)
			userName = "";

		String[] scriptEntries = scriptEntriesData.split(RecordDelimiter);

		for (int c = 0; c < scriptEntries.length; c++)
			data.addElement(scriptEntries[c]);
		FixBlanks();

		ScriptAssistantMouseListener ml = new ScriptAssistantMouseListener();
		datalist.addMouseListener(ml);
		datalist.addMouseMotionListener(ml);
		datalist.addKeyListener(new ScriptAssistantKeyListener());
		frame.addWindowListener(new ScriptAssistantWindowListener());
		frame.addComponentListener(new ScriptAssistantComponentListener());

		frame.setVisible(true);
	}

	public static void main(String[] args) {
		new ScriptAssistant("ITSS Script Assistant");
	}
}
