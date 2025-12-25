package com.clipy.linux;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ClipboardService {
    private final Deque<String> history = new ArrayDeque<>();
    private int maxItems;
    private String lastText = "";

    public ClipboardService(int maxItems) {
        this.maxItems = maxItems;
    }

    public synchronized void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
        while (history.size() > maxItems) {
            history.removeLast();
        }
    }

    public synchronized Deque<String> getHistory() {
        return new ArrayDeque<>(history);
    }

    public synchronized void setClipboardText(String text) {
        StringSelection sel = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
    }

    public void start() {
        Thread t = new Thread(this::loop, "clipboard-watcher");
        t.setDaemon(true);
        t.start();
    }

    private void loop() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        while (true) {
            try {
                if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                    String data = (String) clipboard.getData(DataFlavor.stringFlavor);
                    if (data != null && !data.isEmpty() && !data.equals(lastText)) {
                        addToHistory(data);
                        lastText = data;
                        System.out.println("New clipboard item: " +
                                data.replaceAll("\\s+", " ")
                                        .substring(0, Math.min(40, data.length())));
                    }
                }
                Thread.sleep(300);
            } catch (Exception e) {
                // ignore and keep watching
            }
        }
    }

    private synchronized void addToHistory(String text) {
        history.remove(text);
        history.addFirst(text);
        while (history.size() > maxItems) {
            history.removeLast();
        }
    }

    public synchronized void loadFrom(List<String> items) {
        history.clear();
        for (String s : items) {
            if (s != null && !s.isEmpty()) {
                history.addLast(s);
            }
        }
        if (!history.isEmpty()) {
            lastText = history.peekFirst();
        }
    }

    public synchronized List<String> snapshot() {
        return new ArrayList<>(history);
    }

    public synchronized List<String> getRecent(int max) {
        List<String> list = new ArrayList<>();
        int i = 0;
        for (String s : history) {
            if (i++ >= max) break;
            list.add(s);
        }
        return list;
    }
}
