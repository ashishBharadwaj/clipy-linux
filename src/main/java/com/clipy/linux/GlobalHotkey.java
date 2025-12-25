package com.clipy.linux;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobalHotkey {

    public static void register(Runnable onHotkey) {
        try {
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.WARNING);
            logger.setUseParentHandlers(false);

            GlobalScreen.registerNativeHook();  // install global hook[web:2][web:155]
        } catch (NativeHookException ex) {
            ex.printStackTrace();
            return;
        }

        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent e) {
                System.out.println("Pressed: " + e.getKeyText(e.getKeyCode()) +
                        " mods=" + e.getModifiersText(e.getModifiers()));

                boolean isSpace = e.getKeyCode() == NativeKeyEvent.VC_SPACE;   // Space key[web:144]
                boolean ctrlDown  = (e.getModifiers() & NativeKeyEvent.CTRL_MASK)  != 0;
                boolean shiftDown = (e.getModifiers() & NativeKeyEvent.SHIFT_MASK) != 0;

                if (isSpace && ctrlDown && shiftDown) {
                    System.out.println("Detected Ctrl+Shift+Space");
                    onHotkey.run();
                }
            }


            @Override
            public void nativeKeyReleased(NativeKeyEvent e) { }

            @Override
            public void nativeKeyTyped(NativeKeyEvent e) { }
        });
    }
}
