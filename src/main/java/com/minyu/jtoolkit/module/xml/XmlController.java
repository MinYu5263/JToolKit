package com.minyu.jtoolkit.module.xml;

import atlantafx.base.controls.ToggleSwitch;
import com.minyu.jtoolkit.module.BaseController;
import javafx.animation.PauseTransition;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
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

@Slf4j
@Component
public class XmlController extends BaseController<XmlPersistentState> {

    // XML 的 indent 通常是 int 值
    private final List<IndentOption> indentOptions = List.of(
            new IndentOption("2个空格", "2space", 2),
            new IndentOption("4个空格", "4space", 4)
            // XML Transformer 对 Tab 支持不如空格方便，这里暂时只提供空格
    );
    @FXML
    private TextArea inputArea;
    @FXML
    private TextArea outputArea;
    @FXML
    private ComboBox<IndentOption> indentCombo;
    @FXML
    private ToggleSwitch compactSwitch;
    private PauseTransition formatDebounce;

    @FXML
    public void initView() {
        initIndentCombo();

        formatDebounce = new PauseTransition(Duration.millis(300));
        formatDebounce.setOnFinished(e -> performFormat());

        inputArea.textProperty().addListener((o, old, val) -> formatDebounce.playFromStart());
        indentCombo.valueProperty().addListener((o, old, val) -> performFormat());
        compactSwitch.selectedProperty().addListener((o, old, val) -> performFormat());
    }

    private void initIndentCombo() {
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
        indentCombo.getItems().addAll(indentOptions);
        indentCombo.getSelectionModel().selectFirst();
    }

    private void performFormat() {
        String raw = inputArea.getText();
        if (raw == null || raw.isBlank()) {
            outputArea.clear();
            return;
        }

        try {
            // 1. 先将 String 解析为 Document，同时去除原有的空白节点（清洗）
            Document document = parseAndClean(raw);

            // 2. 根据模式输出
            String result;
            if (compactSwitch.isSelected()) {
                // 压缩模式：indent=no
                result = transformXml(document, 0, false);
            } else {
                // 格式化模式
                IndentOption option = indentCombo.getValue();
                int indentSize = (option != null) ? option.size() : 2;
                result = transformXml(document, indentSize, true);
            }
            outputArea.setText(result);

        } catch (Exception e) {
            // 简单提示，XML 报错通常很长，截取一下
            String msg = e.getMessage();
            outputArea.setText("XML 格式错误: \n" + (msg.length() > 200 ? msg.substring(0, 200) + "" : msg));
        }
    }

    /**
     * 核心逻辑：解析 XML 并移除纯空白文本节点（否则格式化会失效）
     */
    private Document parseAndClean(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true); // 避免命名空间报错
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new InputSource(new StringReader(xml)));

        // 使用 XPath 移除空白节点 (关键步骤，否则输入排版过的 XML 无法重新排版)
        XPath xp = XPathFactory.newInstance().newXPath();
        NodeList nl = (NodeList) xp.evaluate("//text()[normalize-space(.)='']", doc, XPathConstants.NODESET);
        for (int i = 0; i < nl.getLength(); ++i) {
            Node node = nl.item(i);
            node.getParentNode().removeChild(node);
        }
        return doc;
    }

    /**
     * 将 Document 转换为字符串
     */
    private String transformXml(Document doc, int indentAmount, boolean indent) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        // 某些 JDK 版本可能需要 setAttribute("indent-number", indentAmount);
        try {
            tf.setAttribute("indent-number", indentAmount);
        } catch (IllegalArgumentException ignored) {
        }

        Transformer transformer = tf.newTransformer();

        // 设置输出属性
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no"); // 保留头部 <?xml?>
        transformer.setOutputProperty(OutputKeys.INDENT, indent ? "yes" : "no");

        // 标准属性设置缩进大小
        if (indent) {
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indentAmount));
        }

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    @Override
    protected void restoreValues(XmlPersistentState state) {
        inputArea.setText(state.getInputContent());
        compactSwitch.setSelected(state.isCompactMode());
        if (state.getIndentKey() != null) {
            indentOptions.stream()
                    .filter(opt -> opt.key().equals(state.getIndentKey()))
                    .findFirst()
                    .ifPresent(opt -> indentCombo.setValue(opt));
        }
    }

    @Override
    protected XmlPersistentState captureValues() {
        XmlPersistentState state = new XmlPersistentState();
        state.setInputContent(inputArea.getText());
        state.setOutputContent(outputArea.getText());
        state.setCompactMode(compactSwitch.isSelected());
        if (indentCombo.getValue() != null) {
            state.setIndentKey(indentCombo.getValue().key());
        }
        return state;
    }

    @Override
    protected String getViewKey() {
        return "xml_formatter";
    }

    @Override
    protected List<Observable> getObservables() {
        return List.of(inputArea.textProperty(), outputArea.textProperty(),
                indentCombo.valueProperty(), compactSwitch.selectedProperty());
    }

    // 操作按钮
    @FXML
    public void onPaste() {
        Clipboard cb = Clipboard.getSystemClipboard();
        if (cb.hasContent(DataFormat.PLAIN_TEXT)) inputArea.setText(cb.getString());
    }

    @FXML
    public void onClearInput() {
        inputArea.clear();
    }

    @FXML
    public void onCopyOutput() {
        if (outputArea.getText() != null) {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(outputArea.getText());
            Clipboard.getSystemClipboard().setContent(cc);
        }
    }

    // 内部 Record，XML只需要 int 类型的 size
    private record IndentOption(String label, String key, int size) {
    }
}