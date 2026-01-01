package com.minyu.jtoolkit.core.util;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * 全局焦点管理器，点击非输入类控件区域时自动使当前输入框失焦
 */
public class GlobalFocusHandler {

    private static final Set<Class<? extends Node>> INPUT_CLASSES = new HashSet<>();

    static {
        // 初始化默认输入类控件
        INPUT_CLASSES.add(TextField.class);
        INPUT_CLASSES.add(TextArea.class);
        INPUT_CLASSES.add(ComboBox.class);
        INPUT_CLASSES.add(Spinner.class);
        INPUT_CLASSES.add(DatePicker.class);
        INPUT_CLASSES.add(ColorPicker.class);
        INPUT_CLASSES.add(PasswordField.class);
    }

    /**
     * 为Scene启用全局点击失焦功能
     */
    public static void applyTo(Scene scene) {
        if (scene == null) return;

        // 使用addEventFilter在事件分发前拦截
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            Node target = (Node) event.getTarget();

            // 若点击非输入控件且当前有焦点持有者
            if (!isInputComponent(target)) {
                Node focusOwner = scene.getFocusOwner();
                if (focusOwner != null) {
                    // 让根节点获取焦点以清除输入框焦点
                    Parent root = scene.getRoot();
                    if (root != null) {
                        root.requestFocus();
                    }
                }
            }
        });
    }

    /**
     * 判断目标节点是否为输入类控件（含其父节点）
     */
    private static boolean isInputComponent(Node node) {
        while (node != null) {
            // 检查当前节点是否为白名单类型
            for (Class<? extends Node> clazz : INPUT_CLASSES) {
                if (clazz.isInstance(node)) {
                    return true;
                }
            }
            // 向上追溯父节点（处理控件子元素点击场景）
            node = node.getParent();
        }
        return false;
    }

    /**
     * 注册自定义输入控件类型
     */
    public static void registerInputClass(Class<? extends Node> clazz) {
        INPUT_CLASSES.add(clazz);
    }
}