import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    private static String[] servers = {"127.0.0.1", "127.0.0.1", "127.0.0.1", "127.0.0.1"}; //List of servers
    private static int index = 0;
    private static ModelControllerClient client;
    private static JPanel appContainer = new JPanel(new GridLayout());
    private static JFrame frame = new JFrame("WildFly Deployment's Status");
    private static int xDimmension = 1300;
    private static int yDimmension = 775;
    private static JPanel allServers = new JPanel(new GridLayout());
    private static final ArrayList<String> exportList = new ArrayList<>(); //ip and app

    private static void MessageBox(Exception exception) {
        JOptionPane.showMessageDialog(new JFrame(), exception, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static void MessageBox(String information) {
        JOptionPane.showMessageDialog(new JFrame(), information, "Successfully", JOptionPane.INFORMATION_MESSAGE);
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
            index = 0;
            frame.dispose();
            System.gc();
            new Runner();
        }
    }

    static String GetDate() {
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyy_HH-mm-ss");
        Date data = new Date();
        return dateFormat.format(data);
    }

    static HSSFCellStyle enabledStyling(HSSFWorkbook workbook) {
        final HSSFCellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.DOTTED);
        HSSFFont font = workbook.createFont();
        font.setColor(IndexedColors.GREEN.getIndex());
        style.setFont(font);
        return style;
    }

    static HSSFCellStyle disabledStyling(HSSFWorkbook workbook) {
        final HSSFCellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.DOTTED);
        HSSFFont font = workbook.createFont();
        font.setColor(IndexedColors.RED.getIndex());
        style.setFont(font);
        return style;
    }

    static HSSFCellStyle serverStyle(HSSFWorkbook workbook) {
        final HSSFCellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.DOTTED);
        style.setAlignment(HorizontalAlignment.CENTER);
        HSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) 12);
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static class ExportStatus {
        ExportStatus() throws IOException {
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet("Deployment's Status");
            HSSFCellStyle unlockedStyle = workbook.createCellStyle();
            unlockedStyle.setLocked(true);
            sheet.protectSheet("password");
            int countSrvRow = 0;
            for (int i = 0; i < exportList.size(); i++) {
                sheet.setColumnWidth(i, 9000);
            }
            for (String server : servers) {
                countSrvRow++;
                sheet.setDefaultRowHeight((short) 300);
                HSSFRow headerRow = sheet.createRow(countSrvRow);
                HSSFCell headerCell;
                headerCell = headerRow.createCell(0);
                headerCell.setCellStyle(serverStyle(workbook));
                headerCell.setCellValue(server);
                int columns = 1;
                for (String anExportList : exportList) {
                    StringTokenizer tokenizer = new StringTokenizer(anExportList, "<=>");
                    String ip = tokenizer.nextToken();
                    String app = tokenizer.nextToken();
                    String status = tokenizer.nextToken();
                    if (ip.equals(headerCell.toString())) {
                        if (status.contains("true")) {
                            HSSFCell appCell;
                            appCell = headerRow.createCell(columns);
                            appCell.setCellStyle(enabledStyling(workbook));
                            appCell.setCellValue(app);
                            columns++;
                        }
                        if (status.contains("false")) {
                            HSSFCell appCell;
                            appCell = headerRow.createCell(columns);
                            appCell.setCellStyle(disabledStyling(workbook));
                            appCell.setCellValue(app);
                            columns++;
                        }
                    }
                }
            }
            sheet.createFreezePane(1, 1);

            File exportFile = new File("exportstatus_" + GetDate() + ".xls");
            FileOutputStream outFile = new FileOutputStream(exportFile);
            workbook.write(outFile);
            MessageBox("Created File: " + exportFile.getAbsolutePath());
            outFile.close();
        }
    }

    private static class StatusDeployments extends JFrame {
        static HashMap<String, String> applicationMap = new HashMap<>();
        static ArrayList<JPanel> panelsList = new ArrayList<>();

        StatusDeployments() throws IOException, BadLocationException {
            final String username = "admin";
            final String password = "admin";
            applicationMap.clear();
            panelsList.clear();
            appContainer.setSize(xDimmension - 6, yDimmension - 60);
            appContainer.setBorder(BorderFactory.createLineBorder(Color.black));
            int localport = 9990;
            try {
                client = ModelControllerClient.Factory.create(
                        InetAddress.getByName(servers[index]), localport,
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
                MessageBox(new Exception("Error of connection with the server " + servers[index] + " " + e));
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
                serverPanel.setBorder(new TitledBorder(new EtchedBorder(), "Server: " + servers[index]));
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
                    } else if (value.contains("false")) {
                        JPanel disabledPanel = new JPanel(new GridLayout());
                        disabledPanel.setPreferredSize(new Dimension(250, 25));
                        textArea.setBackground(Color.orange);
                        textArea.append(Splitter(key) + "\n");
                        disabledPanel.add(textArea);
                        panelsList.add(disabledPanel);
                    }
                    exportList.add(servers[index] + "<=>" + Splitter(key) + "<=>" + Splitter(value) + "\n");
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

    static class Runner {

        Runner() throws IOException, BadLocationException {

            frame.setSize(xDimmension, yDimmension);
            frame.setLocation(300, 200);
            frame.setLayout(null);
            frame.setResizable(false);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            for (String ignored : servers) {
                new StatusDeployments();
            }
            JScrollPane scrollPane = new JScrollPane(allServers,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            appContainer.add(scrollPane);

            JPanel buttonPanel = new JPanel(new GridLayout());
            JPanel infoPanel = new JPanel(new GridLayout());
            buttonPanel.setBounds(0, 717, xDimmension - 900, 29);
            infoPanel.setBounds(301, 717, xDimmension - 308, 29);
            buttonPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            infoPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            buttonPanel.setBackground(Color.lightGray);
            infoPanel.setBackground(Color.lightGray);
            JButton reloadButton = new JButton();
            JButton exportButton = new JButton();
            exportButton.setText("EXPORT");
            exportButton.setEnabled(true);//
            reloadButton.setText("RELOAD");
            reloadButton.addActionListener(e -> {
                try {
                    new Reload(StatusDeployments.applicationMap, StatusDeployments.panelsList, appContainer, allServers);
                } catch (IOException | BadLocationException e1) {
                    MessageBox(new Exception("Mistake during restart of the program! " + e1));
                }
            });
            exportButton.addActionListener(e -> {
                try {
                    new ExportStatus();
                } catch (IOException e1) {
                    MessageBox(new Exception("Error of the export! " + e1));
                }
            });
            buttonPanel.add(reloadButton);
            buttonPanel.add(exportButton);
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
