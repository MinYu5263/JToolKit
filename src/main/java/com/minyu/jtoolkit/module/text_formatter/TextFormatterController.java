package com.minyu.jtoolkit.module.text_formatter;

import atlantafx.base.controls.ToggleSwitch;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.minyu.jtoolkit.core.component.EnhancedTextArea;
import com.minyu.jtoolkit.module.BaseController;
import javafx.animation.PauseTransition;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

/**
 * 文本格式化工具 — 合并 JSON 和 XML 格式化，通过下拉框切换格式类型，
 * 各自独立保存内容与设置。
 */
@Slf4j
@Component
public class TextFormatterController extends BaseController<TextFormatterPersistentState> {

    @FXML private ComboBox<String> formatTypeCombo;
    @FXML private EnhancedTextArea inputArea;
    @FXML private EnhancedTextArea outputArea;
    @FXML private ComboBox<IndentOption> indentCombo;
    @FXML private ToggleSwitch compactSwitch;

    private PauseTransition formatDebounce;
    private boolean isRestoring = false;

    // ── JSON 缩进选项 ──
    private final List<IndentOption> jsonIndentOptions = List.of(
            new IndentOption("2个空格", 2, JSONWriter.Feature.PrettyFormatWith2Space),
            new IndentOption("4个空格", 4, JSONWriter.Feature.PrettyFormatWith4Space),
            new IndentOption("1个制表符", 0, JSONWriter.Feature.PrettyFormat)
    );

    // ── XML 缩进选项 ──
    private final List<IndentOption> xmlIndentOptions = List.of(
            new IndentOption("2个空格", 2, null),
            new IndentOption("4个空格", 4, null)
    );

    // ── 运行时状态：每种格式独立保存 ──
    private String currentFormat = "JSON";
    private String jsonInput = "";
    private String jsonOutput = "";
    private int jsonIndentIdx = 0;
    private boolean jsonCompact = false;
    private String xmlInput = "";
    private String xmlOutput = "";
    private int xmlIndentIdx = 0;
    private boolean xmlCompact = false;

    @FXML
    public void initView() {
        formatTypeCombo.getItems().addAll("JSON", "XML");

        indentCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(IndentOption item) {
                return item == null ? "" : item.label();
            }
            @Override
            public IndentOption fromString(String string) {
                return null;
            }
        });

        formatDebounce = new PauseTransition(Duration.millis(300));
        formatDebounce.setOnFinished(e -> performFormat());

        inputArea.textProperty().addListener((o, old, val) -> {
            if (isRestoring) return;
            formatDebounce.playFromStart();
        });
        indentCombo.valueProperty().addListener((o, old, val) -> {
            if (isRestoring) return;
            performFormat();
        });
        compactSwitch.selectedProperty().addListener((o, old, val) -> {
            if (isRestoring) return;
            performFormat();
        });
        formatTypeCombo.valueProperty().addListener((obs, old, val) -> {
            if (val == null || isRestoring || val.equals(currentFormat)) return;
            switchFormat(val);
        });

        // 同步滚动
        inputArea.scrollTopProperty().bindBidirectional(outputArea.scrollTopProperty());
        inputArea.scrollLeftProperty().bindBidirectional(outputArea.scrollLeftProperty());

        // 默认 JSON
        indentCombo.getItems().setAll(jsonIndentOptions);
        indentCombo.getSelectionModel().select(0);
        formatTypeCombo.setValue("JSON");
    }

    // ── 格式切换 ──

    /**
     * 切换格式化类型：保存当前 UI 到旧格式的运行时状态，再加载新格式的内容到 UI。
     */
    private void switchFormat(String newFormat) {
        saveCurrentToRuntime();

        // 先清空输入，防止切换控件时触发格式化产生误报错误
        inputArea.clear();
        outputArea.clear();

        if ("XML".equals(newFormat)) {
            indentCombo.getItems().setAll(xmlIndentOptions);
            indentCombo.getSelectionModel().select(xmlIndentIdx);
            compactSwitch.setSelected(xmlCompact);
            currentFormat = newFormat;
            inputArea.setText(xmlInput);
            outputArea.setText(xmlOutput);
        } else {
            indentCombo.getItems().setAll(jsonIndentOptions);
            indentCombo.getSelectionModel().select(jsonIndentIdx);
            compactSwitch.setSelected(jsonCompact);
            currentFormat = newFormat;
            inputArea.setText(jsonInput);
            outputArea.setText(jsonOutput);
        }
    }

    /** 将当前 UI 状态保存到对应格式的运行时字段 */
    private void saveCurrentToRuntime() {
        if ("JSON".equals(currentFormat)) {
            jsonInput = inputArea.getText();
            jsonOutput = outputArea.getText();
            jsonCompact = compactSwitch.isSelected();
            jsonIndentIdx = Math.max(0, indentCombo.getSelectionModel().getSelectedIndex());
        } else {
            xmlInput = inputArea.getText();
            xmlOutput = outputArea.getText();
            xmlCompact = compactSwitch.isSelected();
            xmlIndentIdx = Math.max(0, indentCombo.getSelectionModel().getSelectedIndex());
        }
    }

    // ── 格式化逻辑 ──

    private void performFormat() {
        String raw = inputArea.getText();
        if (raw == null || raw.isBlank()) {
            outputArea.clear();
            return;
        }
        if ("XML".equals(currentFormat)) {
            formatXml(raw);
        } else {
            formatJson(raw);
        }
    }

    private void formatJson(String raw) {
        try {
            Object obj = JSON.parse(raw);
            String result;
            if (compactSwitch.isSelected()) {
                result = JSON.toJSONString(obj);
            } else {
                IndentOption option = indentCombo.getValue();
                JSONWriter.Feature feature = (option != null && option.jsonFeature() != null)
                        ? option.jsonFeature() : JSONWriter.Feature.PrettyFormatWith2Space;
                result = JSON.toJSONString(obj, feature);
            }
            outputArea.setText(result);
        } catch (Exception e) {
            outputArea.setText("JSON 格式错误:\n" + e.getMessage());
        }
    }

    private void formatXml(String raw) {
        try {
            Document doc = parseAndClean(raw);
            String result;
            if (compactSwitch.isSelected()) {
                result = transformXml(doc, 0, false);
            } else {
                IndentOption option = indentCombo.getValue();
                int indentSize = (option != null) ? option.size() : 2;
                result = transformXml(doc, indentSize, true);
            }
            outputArea.setText(result);
        } catch (Exception e) {
            String msg = e.getMessage();
            outputArea.setText("XML 格式错误:\n" + (msg != null && msg.length() > 200 ? msg.substring(0, 200) : msg));
        }
    }

    // ── XML 工具方法 ──

    private Document parseAndClean(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new InputSource(new StringReader(xml)));
        XPath xp = XPathFactory.newInstance().newXPath();
        NodeList nl = (NodeList) xp.evaluate("//text()[normalize-space(.)='']", doc, XPathConstants.NODESET);
        for (int i = 0; i < nl.getLength(); ++i) {
            Node node = nl.item(i);
            node.getParentNode().removeChild(node);
        }
        return doc;
    }

    private String transformXml(Document doc, int indentAmount, boolean indent) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        try {
            tf.setAttribute("indent-number", indentAmount);
        } catch (IllegalArgumentException ignored) {}
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, indent ? "yes" : "no");
        if (indent) {
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indentAmount));
        }
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    // ── 持久化 ──

    @Override
    protected String getViewKey() {
        return "text_formatter";
    }

    @Override
    protected List<Observable> getObservables() {
        return List.of(
                inputArea.textProperty(),
                outputArea.textProperty(),
                indentCombo.valueProperty(),
                compactSwitch.selectedProperty(),
                formatTypeCombo.valueProperty()
        );
    }

    @Override
    protected void restoreValues(TextFormatterPersistentState state) {
        if (state == null) return;

        // 加载持久化数据到运行时字段
        jsonInput = orEmpty(state.getJsonInputContent());
        jsonOutput = orEmpty(state.getJsonOutputContent());
        jsonCompact = state.isJsonCompactMode();
        jsonIndentIdx = findIndentIndex(state.getJsonIndentLabel(), jsonIndentOptions);

        xmlInput = orEmpty(state.getXmlInputContent());
        xmlOutput = orEmpty(state.getXmlOutputContent());
        xmlCompact = state.isXmlCompactMode();
        xmlIndentIdx = findIndentIndex(state.getXmlIndentLabel(), xmlIndentOptions);

        currentFormat = state.getCurrentFormat() != null ? state.getCurrentFormat() : "JSON";

        isRestoring = true;
        try {
            formatTypeCombo.setValue(currentFormat);

            if ("XML".equals(currentFormat)) {
                indentCombo.getItems().setAll(xmlIndentOptions);
                indentCombo.getSelectionModel().select(xmlIndentIdx);
                compactSwitch.setSelected(xmlCompact);
                inputArea.setText(xmlInput);
                outputArea.setText(xmlOutput);
            } else {
                indentCombo.getItems().setAll(jsonIndentOptions);
                indentCombo.getSelectionModel().select(jsonIndentIdx);
                compactSwitch.setSelected(jsonCompact);
                inputArea.setText(jsonInput);
                outputArea.setText(jsonOutput);
            }
        } finally {
            isRestoring = false;
        }
        formatDebounce.playFromStart();
    }

    @Override
    protected TextFormatterPersistentState captureValues() {
        saveCurrentToRuntime();

        TextFormatterPersistentState state = new TextFormatterPersistentState();
        state.setCurrentFormat(currentFormat);

        state.setJsonInputContent(jsonInput);
        state.setJsonOutputContent(jsonOutput);
        state.setJsonIndentLabel(jsonIndentIdx < jsonIndentOptions.size()
                ? jsonIndentOptions.get(jsonIndentIdx).label() : "2个空格");
        state.setJsonCompactMode(jsonCompact);

        state.setXmlInputContent(xmlInput);
        state.setXmlOutputContent(xmlOutput);
        state.setXmlIndentLabel(xmlIndentIdx < xmlIndentOptions.size()
                ? xmlIndentOptions.get(xmlIndentIdx).label() : "2个空格");
        state.setXmlCompactMode(xmlCompact);

        return state;
    }

    // ── 辅助方法 ──

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    private static int findIndentIndex(String label, List<IndentOption> options) {
        if (label == null) return 0;
        for (int i = 0; i < options.size(); i++) {
            if (label.equals(options.get(i).label())) return i;
        }
        return 0;
    }

    /** 缩进选项：标签展示 + 空格数 + JSON 特性（XML 下为 null） */
    private record IndentOption(String label, int size, JSONWriter.Feature jsonFeature) {}
}
