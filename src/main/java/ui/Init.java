package ui;


import file.DependencyManager;
import file.GlobalWurstConfig;
import file.WurstProjectConfig;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class Init {

    static MainWindow mainFrame;

    public static void init(boolean withoutUi, File projectDir) {
        GlobalWurstConfig.load();
        if (withoutUi) {
            boolean wurstUpdt = GlobalWurstConfig.updateAvailable;
            boolean projectUpdt = false;
            // Display dialog asking to update, if updates present
            if (projectDir != null) {
                try {
                    WurstProjectConfig projectConfig = WurstProjectConfig.loadProject(projectDir);
                    projectUpdt = DependencyManager.isUpdateAvailable(projectConfig);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (wurstUpdt || projectUpdt) {
                System.out.println("available");
                int dialogResult = JOptionPane.showConfirmDialog(null, "An update has been found. Would you like to download it now?",
                        "Update available", JOptionPane.YES_NO_OPTION);
                if (dialogResult == JOptionPane.YES_OPTION) {
                    System.out.println("user ordered update");
                }
            }
        } else {
            initUI();
        }
    }

    private static void initUI() {
        mainFrame = new MainWindow();
    }

    public static void log(String message) {
        if (mainFrame != null) {
            mainFrame.ui.jTextArea.append(message);
            mainFrame.ui.jTextArea.setCaretPosition(mainFrame.ui.jTextArea.getText().length() - 1);
        }
    }

    public static void setMaxProgress(int max) {
        mainFrame.ui.progressBar.setMaximum(max);
    }

    public static void setProgress(int current) {
        mainFrame.ui.progressBar.setValue(current);
    }

    public static void setProgress(String state) {
        mainFrame.ui.lblWelcome.setText(state);
    }


    public static void refreshUi() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> mainFrame.ui.refreshComponents());
        }
    }
}
