package com.minyu.jtoolkit.module.regex;

import atlantafx.base.controls.ToggleSwitch;
import com.minyu.jtoolkit.module.BaseController;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class RegexController extends BaseController<RegexPersistentState> {

    @FXML private TextField regexField;
    @FXML private ComboBox<String> templateCombo;
    @FXML private Label matchCountLabel;
    @FXML private StackPane editorContainer;

    @FXML private ToggleSwitch swGlobal;
    @FXML private ToggleSwitch swIgnoreCase;
    @FXML private ToggleSwitch swMultiline;
    @FXML private ToggleSwitch swDotAll;
    @FXML private ToggleSwitch swComments;
    @FXML private ToggleSwitch swUnicode;
    @FXML private ToggleSwitch swCanonEq;

    private CodeArea codeArea;

    private final Map<String, String> regexTemplates = new LinkedHashMap<>();

    private static final String MATCH_CLASS = "regex-match";

    public void initView() {
        initCodeArea();
        initTemplates();
        registerListeners();
    }

    private void initCodeArea() {
        codeArea = new CodeArea();
        codeArea.getStyleClass().addAll("code-area", "regex-editor");

        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        scrollPane.getStyleClass().add("regex-editor-scroll");

        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            double dy = event.getDeltaY();
            double scrollY = scrollPane.estimatedScrollYProperty().getValue();
            double totalH = scrollPane.totalHeightEstimateProperty().getValue();
            double viewH = scrollPane.getHeight();

            boolean atTop = scrollY <= 0 && dy > 0;
            boolean atBottom = scrollY >= totalH - viewH - 1 && dy < 0;
            boolean contentFits = totalH <= viewH;

            if (atTop || atBottom || contentFits) {
                event.consume();
                Node parent = scrollPane.getParent();
                if (parent != null) {
                    Event.fireEvent(parent, event.copyFor(parent, parent));
                }
            }
        });

        editorContainer.getChildren().add(scrollPane);
    }

    private void initTemplates() {
        regexTemplates.put("邮箱", "(?<![A-Za-z0-9._%+-])[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}(?![A-Za-z0-9._%+-])");
        regexTemplates.put("手机号(+86)", "(?<!\\d)(?:\\+?86[-\\s]?)?1[3-9]\\d[-\\s]?\\d{4}[-\\s]?\\d{4}(?!\\d)");
        regexTemplates.put("日期", "(?<!\\d)(?:\\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\\d|3[01])|\\d{4}年(?:0?[1-9]|1[0-2])月(?:0?[1-9]|[12]\\d|3[01])日)(?!\\d)");
        regexTemplates.put("URL", "https?://[A-Za-z0-9.-]+(?::\\d+)?(?:/[A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=%-]*)?");
        regexTemplates.put("金额", "(?:[$¥])\\s?\\d+(?:\\.\\d{1,2})?");
        regexTemplates.put("IPv4", "(?<!\\d)(?:25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(?:\\.(?:25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)){3}(?!\\d)");
        regexTemplates.put("中文字符", "[\\u4e00-\\u9fa5]+");
        regexTemplates.put("居民身份证(18位)", "(?<!\\d)[1-9]\\d{5}(?:18|19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx](?!\\d)");

        templateCombo.getItems().addAll(regexTemplates.keySet());
        templateCombo.setOnAction(e -> {
            String selected = templateCombo.getValue();
            if (selected != null && regexTemplates.containsKey(selected)) {
                regexField.setText(regexTemplates.get(selected));
            }
        });
    }

    private void registerListeners() {
        codeArea.textProperty().addListener((obs, old, val) -> computeHighlighting());
        regexField.textProperty().addListener((obs, old, val) -> computeHighlighting());

        List.of(swGlobal, swIgnoreCase, swMultiline, swDotAll, swComments, swUnicode, swCanonEq)
                .forEach(sw -> sw.selectedProperty().addListener(obs -> computeHighlighting()));
    }

    private void computeHighlighting() {
        String text = codeArea.getText();
        String regex = regexField.getText();

        matchCountLabel.setText("0 matches");

        if (text == null || text.isEmpty() || regex == null || regex.isEmpty()) {
            codeArea.clearStyle(0, text.length());
            return;
        }

        try {
            int flags = 0;
            if (swIgnoreCase.isSelected()) flags |= Pattern.CASE_INSENSITIVE;
            if (swMultiline.isSelected()) flags |= Pattern.MULTILINE;
            if (swDotAll.isSelected()) flags |= Pattern.DOTALL;
            if (swComments.isSelected()) flags |= Pattern.COMMENTS;
            if (swCanonEq.isSelected()) flags |= Pattern.CANON_EQ;
            if (swUnicode.isSelected()) {
                flags |= Pattern.UNICODE_CHARACTER_CLASS;
                flags |= Pattern.UNICODE_CASE;
            }

            Pattern pattern = Pattern.compile(regex, flags);
            Matcher matcher = pattern.matcher(text);

            StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
            int lastEnd = 0;
            int count = 0;
            boolean global = swGlobal.isSelected();

            while (matcher.find()) {
                spansBuilder.add(Collections.emptyList(), matcher.start() - lastEnd);

                spansBuilder.add(Collections.singleton(MATCH_CLASS), matcher.end() - matcher.start());

                lastEnd = matcher.end();
                count++;

                if (!global) break;
            }

            spansBuilder.add(Collections.emptyList(), text.length() - lastEnd);

            StyleSpans<Collection<String>> spans = spansBuilder.create();
            codeArea.setStyleSpans(0, spans);

            matchCountLabel.setText(count + " matches");

        } catch (PatternSyntaxException e) {
            codeArea.clearStyle(0, text.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String getViewKey() {
        return "regex_tester";
    }

    @Override
    protected List<Observable> getObservables() {
        return List.of(
                regexField.textProperty(),
                codeArea.textProperty(),
                swGlobal.selectedProperty(),
                swIgnoreCase.selectedProperty(),
                swMultiline.selectedProperty(),
                swDotAll.selectedProperty(),
                swComments.selectedProperty(),
                swUnicode.selectedProperty(),
                swCanonEq.selectedProperty()
        );
    }

    @Override
    protected void restoreValues(RegexPersistentState state) {
        if (state == null) return;
        regexField.setText(state.getRegexPattern());

        if (state.getSourceText() != null) {
            codeArea.replaceText(state.getSourceText());
        }

        swGlobal.setSelected(state.isGlobal());
        swIgnoreCase.setSelected(state.isIgnoreCase());
        swMultiline.setSelected(state.isMultiline());
        swDotAll.setSelected(state.isDotAll());
        swComments.setSelected(state.isComments());
        swUnicode.setSelected(state.isUnicode());
        swCanonEq.setSelected(state.isCanonEq());

        Platform.runLater(this::computeHighlighting);
    }

    @Override
    protected RegexPersistentState captureValues() {
        return new RegexPersistentState(
                regexField.getText(),
                codeArea.getText(),
                swGlobal.isSelected(),
                swIgnoreCase.isSelected(),
                swMultiline.isSelected(),
                swDotAll.isSelected(),
                swComments.isSelected(),
                swUnicode.isSelected(),
                swCanonEq.isSelected()
        );
    }
}
