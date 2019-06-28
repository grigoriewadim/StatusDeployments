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
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.text.BadLocationException;
import java.awt.*;
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
    private static JFrame finderFrame = new JFrame("Find Modules");
    private static JFrame warningFrame = new JFrame("Предупреждение!");
    private static int xDimmension = 1300;
    private static int yDimmension = 775;
    private static JPanel allServers = new JPanel(new GridLayout());
    private static ArrayList<String> hostList = new ArrayList<>();
    private static int counter;

    static class ReadConfig { //Читаем конфиг
        static String login;
        static String password;

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

    static class WarningFrameReload {
        WarningFrameReload(String warningHost) {
            Font font = new Font("Arial Black", Font.BOLD, 11);
            warningFrame.setSize(500, 90);
            JTextArea warningField = new JTextArea();
            warningField.setFont(font);
            warningField.setEnabled(false);
            warningField.setText("Внимание! На хосте " + warningHost + " запущено 5 и более модулей. " + "\n" +
                    "Это может повлиять на работоспособность сервера.");
            warningFrame.add(warningField);
            warningFrame.setLocationByPlatform(true);
            warningFrame.setVisible(true);
        }
    }

    private static void MessageBox(Exception exception) {
        JOptionPane.showMessageDialog(new JFrame(), exception, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static void MessageBox() {
        JOptionPane.showMessageDialog(new JFrame(), "Не корректный config", "Successfully", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String Splitter(String string) {
        return string.replace("[", "").replace("]", "").replace("\"", "").replace(",", "");
    }

    private static class Reload {
        Reload(HashMap<String, String> map, ArrayList list, JPanel appContainer, JPanel serverPanel) throws IOException, BadLocationException {
            map.clear();
            list.clear();
            appContainer.removeAll();
            serverPanel.removeAll();
            hostList.clear();
            index = 0;
            warningFrame.dispose();
            finderFrame.dispose();
            frame.dispose();
            System.gc();
            new Runner();

        }
    }

    private static class StatusDeployments extends JFrame {
        static HashMap<String, String> applicationMap = new HashMap<>();
        static ArrayList<JPanel> panelsList = new ArrayList<>();
        String[] transportModules = {"custodian", "dpl", "logger", "seap", "transport"};

        StatusDeployments() throws IOException, BadLocationException {
            final String username = ReadConfig.login;
            final String password = ReadConfig.password;
            applicationMap.clear();
            panelsList.clear();
            appContainer.setSize(xDimmension - 6, yDimmension - 60);
            appContainer.setBorder(BorderFactory.createLineBorder(Color.black));
            int localport = 9990;
            counter = 0;
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
                    applicationMap.put(Arrays.toString(namesList.get("runtime-name").toString().split(",")),
                            Arrays.toString(namesList.get("enabled").toString().split(",")));
                }
                JPanel serverPanel = new JPanel(new FlowLayout());
                serverPanel.setBorder(new TitledBorder(new EtchedBorder(), "Server: " + hostList.get(index)));
                for (String transportModule : transportModules) {
                    applicationMap.entrySet().removeIf(entry -> entry.getKey().contains(transportModule));
                }
                for (Map.Entry<String, String> entry : applicationMap.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    JTextArea textArea = new JTextArea();
                    textArea.setEditable(false);
                    if (value.contains("true")) {
                        JPanel enabledPanel = new JPanel(new GridLayout());
                        enabledPanel.setPreferredSize(new Dimension(250, 25));
                        textArea.setBackground(Color.cyan);
                        textArea.append(Splitter(key) + "\n");
                        enabledPanel.add(textArea);
                        panelsList.add(enabledPanel);
                        counter++;
                    } else if (value.contains("false")) {
                        JPanel disabledPanel = new JPanel(new GridLayout());
                        disabledPanel.setPreferredSize(new Dimension(250, 25));
                        textArea.setBackground(Color.orange);
                        textArea.append(Splitter(key) + "\n");
                        disabledPanel.add(textArea);
                        panelsList.add(disabledPanel);
                    }
                }
                for (int i = 0; i < applicationMap.size(); i++) {
                    serverPanel.add(panelsList.get(i));
                }
                allServers.setPreferredSize(new Dimension(xDimmension - 7, panelsList.size() * 35));
                allServers.add(serverPanel);

            } catch (IllegalArgumentException e) {
                MessageBox(new Exception("I/O Error " + e));
            } catch (java.net.ConnectException e) {
                MessageBox(new Exception("Timeout error... " + e));
            } catch (IOException e) {
                MessageBox(e);
            }
            index++;
            frame.add(appContainer);
            client.close();
        }
    }

    static String FinderDialog() {
        return JOptionPane.showInputDialog(null, "What's find ?", "Find Module", JOptionPane.PLAIN_MESSAGE);
    }

    static class FindText {  //Класс для поиска запрашиваемого текста в JTextArea
        FindText() {
            String findText = FinderDialog();
            if (findText.equals("")) {
                JOptionPane.showMessageDialog(null, " Null String! ");
            }
            finderFrame = new JFrame("Modules");
            finderFrame.setSize(300, 500);
            finderFrame.setLocation(600,300);
            JPanel finderPanel = new JPanel();





            finderFrame.add(finderPanel);
            finderFrame.setVisible(true);
        }
    }

    static class Runner {

        Runner() throws IOException, BadLocationException {
            try {
                new ReadConfig();
            } catch (Exception e) {
                MessageBox(e);
            }
            frame.setSize(xDimmension, yDimmension);
            frame.setLocation(300, 200);
            frame.setLayout(null);
            frame.setResizable(false);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            for (String counterHosts : hostList) {
                new StatusDeployments();
                //if (counter > 5) new WarningFrameReload(counterHosts);
            }
            JScrollPane scrollPane = new JScrollPane(allServers,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            appContainer.add(scrollPane);
            JPanel buttonPanel = new JPanel(new GridLayout());
            JPanel infoPanel = new JPanel(new GridLayout());
            buttonPanel.setBounds(0, 717, xDimmension - 1000, 29);
            infoPanel.setBounds(301, 717, xDimmension - 308, 29);
            buttonPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            infoPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            buttonPanel.setBackground(Color.lightGray);
            infoPanel.setBackground(Color.lightGray);
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
                new FindText();
            });

            buttonPanel.add(reloadButton);
            buttonPanel.add(findButton);
            frame.add(buttonPanel);
            frame.add(infoPanel);
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