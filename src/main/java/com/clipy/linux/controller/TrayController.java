package com.clipy.linux.controller;

import com.clipy.linux.model.Snippet;
import com.clipy.linux.model.SnippetsModel;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

public class TrayController {

    private final Runnable onShowHistory;
    private final Runnable onShowPreferences;
    private final Runnable onEditSnippets;
    private final Consumer<String> onSelectItem;
    private final IntFunction<List<String>> getRecentItems;
    private final IntSupplier getMaxTrayItems;

    private final SnippetsModel snippetsModel;

    private TrayIcon trayIcon;

    public TrayController(Runnable onShowHistory,
                          Runnable onShowPreferences,
                          Runnable onEditSnippets,
                          Consumer<String> onSelectItem,
                          IntFunction<List<String>> getRecentItems,
                          IntSupplier getMaxTrayItems,
                          SnippetsModel snippetsModel) {
        this.onShowHistory = onShowHistory;
        this.onShowPreferences = onShowPreferences;
        this.onEditSnippets = onEditSnippets;
        this.onSelectItem = onSelectItem;
        this.getRecentItems = getRecentItems;
        this.getMaxTrayItems = getMaxTrayItems;
        this.snippetsModel = snippetsModel;
    }

    public void init() throws AWTException {
        if (!SystemTray.isSupported()) {
            System.out.println("System tray not supported on this system.");
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();
        Image image = createDefaultImage();

        PopupMenu popup = new PopupMenu();
        rebuildMenu(popup);

        trayIcon = new TrayIcon(image, "Clipy Linux", popup);
        trayIcon.setImageAutoSize(true);

        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    rebuildMenu(popup);
                }
            }
        });

        tray.add(trayIcon);
    }

    private void rebuildMenu(PopupMenu popup) {
        popup.removeAll();

        MenuItem showHistoryItem = new MenuItem("Open full history...");
        MenuItem editSnippetsItem = new MenuItem("Edit snippets...");
        MenuItem prefsItem = new MenuItem("Preferences...");
        MenuItem exitItem = new MenuItem("Quit");

        showHistoryItem.addActionListener(e -> onShowHistory.run());
        editSnippetsItem.addActionListener(e -> onEditSnippets.run());
        prefsItem.addActionListener(e -> onShowPreferences.run());
        exitItem.addActionListener(e -> System.exit(0));

        popup.add(showHistoryItem);
        popup.add(editSnippetsItem);
        popup.add(prefsItem);
        popup.addSeparator();

        // ----- history groups -----
        int maxTray = getMaxTrayItems.getAsInt();
        List<String> recent = getRecentItems.apply(maxTray);

        int groupSize = 10;
        int total = recent.size();
        int start = 0;
        while (start < total) {
            int end = Math.min(start + groupSize, total);

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
                int displayIndex = i + 1;
                MenuItem mi = new MenuItem(displayIndex + ": " + display);
                String value = itemText;
                mi.addActionListener(e -> pasteText(value));
                groupMenu.add(mi);
            }

            popup.add(groupMenu);
            start += groupSize;
        }

        // ----- snippets section -----
        popup.addSeparator();
        Menu snippetsMenu = new Menu("Snippets");
        if (snippetsModel.getFolders().isEmpty()) {
            MenuItem empty = new MenuItem("(no snippets yet)");
            empty.setEnabled(false);
            snippetsMenu.add(empty);
        } else {
            for (String folder : snippetsModel.getFolders()) {
                Menu folderMenu = new Menu(folder);
                for (Snippet snippet : snippetsModel.getSnippetsInFolder(folder)) {
                    String label = snippet.getName();
                    if (label == null || label.isBlank()) {
                        label = abbreviate(snippet.getContent(), 50);
                    }
                    MenuItem mi = new MenuItem(label);
                    String text = snippet.getContent();
                    mi.addActionListener(e -> pasteText(text));
                    folderMenu.add(mi);
                }
                snippetsMenu.add(folderMenu);
            }
        }
        popup.add(snippetsMenu);

        if (total > 0 || !snippetsModel.getFolders().isEmpty()) {
            popup.addSeparator();
        }
        popup.add(exitItem);
    }

    private String abbreviate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max - 3) + "...";
    }

    /**
     * Draw a simple clipboard icon:
     * - rounded rectangle for the board
     * - smaller clip shape on top
     */
    private Image createDefaultImage() {
        int size = 16;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // clear
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, size, size);
        g.setComposite(AlphaComposite.SrcOver);

        // board
        int boardX = 3;
        int boardY = 4;
        int boardW = 10;
        int boardH = 11;

        g.setColor(new Color(240, 240, 240));
        g.fillRoundRect(boardX, boardY, boardW, boardH, 3, 3);
        g.setColor(new Color(120, 120, 120));
        g.drawRoundRect(boardX, boardY, boardW, boardH, 3, 3);

        // clip at top
        int clipW = 8;
        int clipH = 3;
        int clipX = boardX + (boardW - clipW) / 2;
        int clipY = 1;

        g.setColor(new Color(210, 210, 210));
        g.fillRoundRect(clipX, clipY, clipW, clipH, 3, 3);
        g.setColor(new Color(100, 100, 100));
        g.drawRoundRect(clipX, clipY, clipW, clipH, 3, 3);

        // inner "paper" lines
        g.setColor(new Color(180, 180, 180));
        for (int y = boardY + 3; y <= boardY + boardH - 2; y += 3) {
            g.drawLine(boardX + 2, y, boardX + boardW - 2, y);
        }

        g.dispose();
        return img;
    }

    private void pasteText(String text) {
        if (text == null) {
            return;
        }

        onSelectItem.accept(text);

        try {
            Robot robot = new Robot();
            robot.setAutoDelay(20);
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_CONTROL);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
