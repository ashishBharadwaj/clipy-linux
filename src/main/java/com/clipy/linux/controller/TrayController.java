package com.clipy.linux.controller;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

public class TrayController {

    private final Runnable onShowMain;
    private final Runnable onShowHistory;
    private final Runnable onShowPreferences;
    private final Consumer<String> onSelectItem;
    private final IntFunction<List<String>> getRecentItems;
    private final IntSupplier getMaxTrayItems;
    private TrayIcon trayIcon;

    public TrayController(Runnable onShowMain,
                          Runnable onShowHistory,
                          Runnable onShowPreferences,
                          Consumer<String> onSelectItem,
                          IntFunction<List<String>> getRecentItems,
                          IntSupplier getMaxTrayItems) {
        this.onShowMain = onShowMain;
        this.onShowHistory = onShowHistory;
        this.onShowPreferences = onShowPreferences;
        this.onSelectItem = onSelectItem;
        this.getRecentItems = getRecentItems;
        this.getMaxTrayItems = getMaxTrayItems;
    }

    public void init() throws AWTException {
        if (!SystemTray.isSupported()) {
            System.out.println("System tray not supported on this system.");
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();
        Image image = createDefaultImage();

        PopupMenu popup = new PopupMenu();
        rebuildRecentItems(popup);

        trayIcon = new TrayIcon(image, "Clipy Linux", popup);
        trayIcon.setImageAutoSize(true);

        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    rebuildRecentItems(popup);
                }
            }
        });

        tray.add(trayIcon);
    }

    private void rebuildRecentItems(PopupMenu popup) {
        popup.removeAll();

        MenuItem showMainItem = new MenuItem("Show main window");
        MenuItem showHistoryItem = new MenuItem("Open full history...");
        MenuItem prefsItem = new MenuItem("Preferences...");
        MenuItem exitItem = new MenuItem("Quit");

        showMainItem.addActionListener(e -> onShowMain.run());
        showHistoryItem.addActionListener(e -> onShowHistory.run());
        prefsItem.addActionListener(e -> onShowPreferences.run());
        exitItem.addActionListener(e -> System.exit(0));

        popup.add(showMainItem);
        popup.add(showHistoryItem);
        popup.add(prefsItem);
        popup.addSeparator();

        int maxTray = getMaxTrayItems.getAsInt();
        List<String> recent = getRecentItems.apply(maxTray);

        int groupSize = 10;
        int total = recent.size();
        int start = 0;
        while (start < total) {
            int end = Math.min(start + groupSize, total);

            // 1-based labels: 1-10, 11-20, ...
            int labelStart = start + 1;
            int labelEnd = end;
            String label = labelStart + " - " + labelEnd;

            Menu groupMenu = new Menu(label);

            for (int i = start; i < end; i++) {
                String itemText = recent.get(i);
                String display = itemText.replaceAll("\\s+", " ");
                if (display.length() > 50) {
                    display = display.substring(0, 47) + "...";
                }
                int displayIndex = i + 1; // 1-based index for display
                MenuItem mi = new MenuItem(displayIndex + ": " + display);
                String value = itemText;
                mi.addActionListener(e -> onSelectItem.accept(value));
                groupMenu.add(mi);
            }

            popup.add(groupMenu);
            start += groupSize;
        }

        if (total > 0) {
            popup.addSeparator();
        }
        popup.add(exitItem);
    }

    private Image createDefaultImage() {
        int size = 16;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, size, size);
        g.setColor(Color.WHITE);
        g.drawString("C", 4, 12);
        g.dispose();
        return img;
    }
}
