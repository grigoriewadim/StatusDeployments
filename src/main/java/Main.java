import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.Operations;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Main {

    private static int index = 0;
    private static ModelControllerClient client;
    private static JPanel appContainer = new JPanel(new GridLayout());
    private static JFrame frame = new JFrame("WildFly Deployment's Status");
    private static JFrame warningFrame = new JFrame("Warning!");
    private static int xDimmension = 1100;
    private static int yDimmension = 777;
    private static JPanel allServers = new JPanel();
    private static ArrayList<String> hostList = new ArrayList<>();
    private static JPanel buttonPanel = new JPanel(new GridLayout());
    private static JPanel functionPanel = new JPanel(new GridLayout());
    private static JPanel selectedPanel = new JPanel(new GridLayout());
    private static String clickedAppName = null;
    private static String clickedHostName = null;
    private static final JLabel choosedLabel = new JLabel();
    private static HashMap<String, Object> checkServers = new HashMap<>();

    static class ReadConfig { //Читаем конфиг
        static String login;
        static String password;
        static int port;

        ReadConfig() throws Exception {
            try {
                String filename = "config.cfg";
                BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!Character.isLetter(line.charAt(0))) {
                        hostList.add(line); //Добавляем в список
                    } else {
                        if (line.contains("login")) {
                            login = line.substring(line.indexOf(":") + 1);
                        } else if (line.contains("password")) {
                            password = line.substring(line.indexOf(":") + 1);
                        } else if (line.contains("port")) {
                            port = Integer.parseInt(line.substring(line.indexOf(":") + 1));
                        } else {
                            MessageBox();
                        }
                    }
                }
                reader.close();
            } catch (FileNotFoundException e) {
                MessageBox(new Exception("Не найден файл!"));
                System.exit(0);
            } catch (NullPointerException e) {
                MessageBox(new Exception(e));
                System.exit(0);
            }
        }
    }

    private static void MessageBox(Exception exception) {
        JOptionPane.showMessageDialog(new JFrame(), exception, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static void MessageBox() {
        JOptionPane.showMessageDialog(new JFrame(), "Не корректный config", "Successfully", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void MessageBox(String threads) {
        JOptionPane.showMessageDialog(new JFrame(), "Выполнение", "Выполняется...", JOptionPane.WARNING_MESSAGE);
    }

    private static String Splitter(String string) {
        return string.replace("[", "").replace("]", "").replace("\"", "").replace(",", "");
    }

    private static JPanel serverBoxes(ArrayList<String> hosts) {
        JPanel checkPanel = new JPanel();
        checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.Y_AXIS));
        for (String element : hosts) {
            JCheckBox sBoxes = new JCheckBox(element);
            checkPanel.add(sBoxes);
            sBoxes.addChangeListener(e -> {
                if (sBoxes.isSelected()) {
                    checkServers.put(element, "");
                }
            });
        }
        return checkPanel;
    }

    private static String ShortName(String runtimeName) {
        int location = runtimeName.lastIndexOf("-");
        return runtimeName.substring(0, location).replaceAll("\\s+", "");
    }

    static class DeployApplicationUI {
        private JButton browseButton = new JButton();
        private JCheckBox statusBox = new JCheckBox();
        private String checkFile = "";
        JTextField nameField = new JTextField();
        JTextField runtimenameField = new JTextField();
        static Boolean status = true;
        static File warFile;

        DeployApplicationUI() {
            Object[] fields = {
                    "", browseButton,
                    "Server", serverBoxes(hostList),
                    "Name", nameField,
                    "Runtime-Name:", runtimenameField,
                    "Enabled?", statusBox,
            };
            browseButton.setText("Choose war File...");
            browseButton.addActionListener(e -> {
                JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
                jfc.setDialogTitle("Choose File");
                jfc.setAcceptAllFileFilterUsed(false);
                FileNameExtensionFilter filter = new FileNameExtensionFilter("WildFly Deployments", "war", "ear");
                jfc.addChoosableFileFilter(filter);
                int returnValue = jfc.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    warFile = jfc.getSelectedFile();
                }
                try {
                    checkFile = warFile.getName();
                    nameField.setText(checkFile);
                    runtimenameField.setText(checkFile);
                } catch (NullPointerException e1) {
                    MessageBox(new Exception(e1));
                }
            });

            JOptionPane.showMessageDialog(null, fields, "Add application", JOptionPane.PLAIN_MESSAGE);
            statusBox.addChangeListener(e -> {
                if (e.getSource() == statusBox) {
                    status = true;
                }
            });

            //Передать в переменну server (та что в цикле) значения из CheckBox
            String deployment_name = nameField.getText();
            String deployment_runtimeName = runtimenameField.getText();
            String request = "deploy " + DeployApplicationUI.warFile.getAbsolutePath() +
                    " --name=" + deployment_name + " --runtime-name=" + deployment_runtimeName + " --" + DeployApplicationUI.status;
            for (Map.Entry<String, Object> entry : checkServers.entrySet()) {
                String ip = entry.getKey();
                //new ControllerClient(ip, ReadConfig.port, request);
                System.out.println(ip);
            }
        }
    }

    private static class Reload {
        Reload(HashMap<String, String> map, ArrayList list, JPanel appContainer, JPanel serverPanel) throws IOException, BadLocationException {
            map.clear();
            list.clear();
            appContainer.removeAll();
            serverPanel.removeAll();
            buttonPanel.removeAll();
            functionPanel.removeAll();
            selectedPanel.removeAll();
            hostList.clear();
            index = 0;
            warningFrame.dispose();
            frame.dispose();
            System.gc();
            new Runner();
        }
    }

    private static class StatusDeployments extends JFrame {
        static HashMap<String, String> applicationMap = new HashMap<>();
        static ArrayList<JPanel> panelsList = new ArrayList<>();
        String[] transportModules = {"custodian", "dpl", "logger", "seap", "transport"};
        JPanel serverPanel;
        static String hostClick;
        static String appClick;

        JPanel CreateTextField(String status, String key) {
            JTextArea textArea = new JTextArea();
            textArea.setEditable(false);
            JPanel applicationPanel = new JPanel(new GridLayout());
            if (status.contains("true")) {
                textArea.setBackground(Color.cyan);
            } else textArea.setBackground(Color.ORANGE);
            applicationPanel.setPreferredSize(new Dimension(220, 25));
            textArea.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent mouseEvent) {
                    super.mouseClicked(mouseEvent);
                    clickedAppName = textArea.getText();
                    hostClick = clickedHostName;
                    appClick = clickedAppName;
                    choosedLabel.setFont(new Font("Arial", Font.BOLD, 12));
                    choosedLabel.setText("App: " + clickedAppName.replaceAll("\\s+", "") + "Host: " + clickedHostName.replaceAll("\\s+", ""));
                    selectedPanel.add(choosedLabel);
                    if (textArea.getBackground().equals((Color.cyan))) {
                        Runner.enabledButton.setEnabled(false);
                        Runner.disabledButton.setEnabled(true);
                    } else if (textArea.getBackground().equals((Color.orange))) {
                        Runner.enabledButton.setEnabled(true);
                        Runner.disabledButton.setEnabled(false);
                    }
                    Runner.removeButton.setEnabled(true);
                }
            });
            selectedPanel.removeAll();
            clickedHostName = "";
            return applicationPanel;
        }

        StatusDeployments() throws IOException, BadLocationException {
            final String username = ReadConfig.login;
            final String password = ReadConfig.password;
            applicationMap.clear();
            panelsList.clear();
            appContainer.setSize(xDimmension, yDimmension - 60);
            appContainer.setBorder(BorderFactory.createLineBorder(Color.black));
            int localport = ReadConfig.port;
            try {
                client = ModelControllerClient.Factory.create(
                        InetAddress.getByName(String.valueOf(hostList.get(index))), localport,
                        callbacks -> {

                            for (Callback current : callbacks) {
                                if (current instanceof NameCallback) {
                                    NameCallback ncb = (NameCallback) current;
                                    ncb.setName(username);
                                } else if (current instanceof PasswordCallback) {
                                    PasswordCallback pcb = (PasswordCallback) current;
                                    pcb.setPassword((password).toCharArray());
                                } else if (current instanceof RealmCallback) {
                                    RealmCallback rcb = (RealmCallback) current;
                                    rcb.setText(rcb.getDefaultText());
                                } else {
                                    throw new UnsupportedCallbackException(current);
                                }
                            }
                        });
            } catch (java.net.UnknownHostException e) {
                MessageBox(new Exception("Error of connection with the server " + hostList.get(index) + " " + e));
            } catch (java.lang.NullPointerException e) {
                MessageBox(new Exception("Sorry! Error... " + e));
            }
            try {
                final org.jboss.dmr.ModelNode names = Operations.createOperation("read-children-resources");
                names.get(ClientConstants.CHILD_TYPE).set(ClientConstants.DEPLOYMENT);
                final org.jboss.dmr.ModelNode nameResult = client.execute(names);
                final org.jboss.dmr.ModelNode deployments = Operations.readResult(nameResult);
                for (String name : deployments.keys()) {
                    final org.jboss.dmr.ModelNode namesList = deployments.get(name);
                    applicationMap.put(Arrays.toString(namesList.get("runtime-name").toString().split(".war")),
                            Arrays.toString(namesList.get("enabled").toString().split(",")));
                }
                serverPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
                serverPanel.setBorder(new TitledBorder(new EtchedBorder(), "Server: " + hostList.get(index)));
                serverPanel.setName(hostList.get(index));
                serverPanel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent mouseEvent) {
                        super.mouseEntered(mouseEvent);
                        clickedHostName = serverPanel.getName();
                    }
                });
                for (String transportModule : transportModules) {
                    applicationMap.entrySet().removeIf(entry -> entry.getKey().contains(transportModule));
                }
                for (Map.Entry<String, String> entry : applicationMap.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    JTextArea textArea = new JTextArea();
                    textArea.setEditable(false);
                    panelsList.add(CreateTextField(value, key + "\n"));
                }
                for (int i = 0; i < applicationMap.size(); i++) {
                    serverPanel.add(panelsList.get(i));
                }
                allServers.setLayout(new GridLayout());
                allServers.setPreferredSize(new Dimension(400, panelsList.size() * 35));
                allServers.add(serverPanel);

            } catch (IllegalArgumentException e) {
                MessageBox(new Exception("I/O Error " + e));
            } catch (java.net.ConnectException e) {
                MessageBox(new Exception("Timeout error... " + e));
            } catch (IOException e) {
                MessageBox(new Exception("Connection error... " + e));
            }
            index++;
            frame.add(appContainer);
            client.close();
        }
    }

    static String FinderDialog() {
        return JOptionPane.showInputDialog(null, "What's find ?", "Find Module", JOptionPane.PLAIN_MESSAGE);
        //new FindTextDialog(String text); - Не реализован
    }

    static class FindTextDialog {

    }

    static class Runner {
        static JButton enabledButton = new JButton("Enable");
        static JButton disabledButton = new JButton("Disable");
        static JButton removeButton = new JButton("Remove");

        Runner() throws IOException, BadLocationException {
            try {
                new ReadConfig();
            } catch (Exception e) {
                MessageBox(e);
            }
            frame.setSize(xDimmension, yDimmension);
            frame.setLocation(100, 200);
            frame.setLayout(null);
            frame.setResizable(false);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            for (String ignored : hostList) {
                new StatusDeployments();
            }
            JScrollPane scrollPane = new JScrollPane(allServers,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            appContainer.add(scrollPane);
            buttonPanel.setBounds(0, 717, xDimmension - 900, 28);
            functionPanel.setBounds(210, 717, xDimmension - 600, 28);
            selectedPanel.setBounds(730, 717, 370, 28);
            JButton reloadButton = new JButton("RELOAD");
            reloadButton.addActionListener(e -> {
                try {
                    new Reload(StatusDeployments.applicationMap, StatusDeployments.panelsList, appContainer, allServers);
                } catch (IOException | BadLocationException e1) {
                    MessageBox(new Exception("Mistake during restart of the program! " + e1));
                }
            });
            JButton findButton = new JButton("FIND");
            findButton.addActionListener(e -> {
                new FindTextDialog();
            });
            findButton.setEnabled(false);

            enabledButton.setEnabled(false);
            enabledButton.addActionListener(e -> {
                String request = "/deployment=" + ShortName(clickedAppName) + " /:deploy";
                new ControllerClient(StatusDeployments.hostClick, ReadConfig.port, request);
            });
            disabledButton.setEnabled(false);
            disabledButton.addActionListener(e -> {
                MessageBox("thread");
                new Thread(() -> {
                    String request = "/deployment=" + ShortName(clickedAppName) + "/:undeploy";
                    new ControllerClient(StatusDeployments.hostClick, ReadConfig.port, request);
                }).start();
            });
            removeButton.setEnabled(false);
            removeButton.addActionListener(e -> {
                MessageBox("thread");
                new Thread(() -> {
                    String request = "undeploy --name " + ShortName(clickedAppName);
                    new ControllerClient(StatusDeployments.hostClick, ReadConfig.port, request);
                }).start();
            });

            JButton addButton = new JButton("Add");
            addButton.setEnabled(true);
            addButton.addActionListener(e -> {
                new DeployApplicationUI();
            });

            buttonPanel.add(reloadButton);
            buttonPanel.add(findButton);
            functionPanel.add(enabledButton);
            functionPanel.add(disabledButton);
            functionPanel.add(removeButton);
            functionPanel.add(addButton);
            frame.add(buttonPanel);
            frame.add(functionPanel);
            frame.add(selectedPanel);
            frame.setVisible(true);
        }
    }

    public static void main(String[] args) throws IOException, BadLocationException {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
            UIManager.put("nimbusBlueGrey", new Color(101, 199, 101));
        } catch (UnsupportedLookAndFeelException e) {
            MessageBox(e);
        }
        new Runner();
    }
}