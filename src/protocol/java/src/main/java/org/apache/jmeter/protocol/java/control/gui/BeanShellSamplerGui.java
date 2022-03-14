/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jmeter.protocol.java.control.gui;

import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.jmeter.gui.TestElementMetadata;
import org.apache.jmeter.gui.util.FilePanelEntry;
import org.apache.jmeter.gui.util.JSyntaxTextArea;
import org.apache.jmeter.gui.util.JTextScrollPane;
import org.apache.jmeter.protocol.java.sampler.BeanShellSampler;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.BooleanProperty;
import org.apache.jmeter.util.JMeterUtils;

@TestElementMetadata(labelResource = "bsh_sampler_title")
public class BeanShellSamplerGui extends AbstractSamplerGui {

    private static final long serialVersionUID = 240L;

    private JCheckBox resetInterpreter;// reset the bsh.Interpreter before each execution

    private final FilePanelEntry filename = new FilePanelEntry(JMeterUtils.getResString("bsh_script_file"),".bsh"); // script file name (if present)

    private JTextField parameters;// parameters to pass to script file (or script)

    private JSyntaxTextArea scriptField;// script area

    public BeanShellSamplerGui() {
        init();
    }

    @Override
    public void configure(TestElement element) {
        scriptField.setInitialText(element.getPropertyAsString(BeanShellSampler.SCRIPT));
        scriptField.setCaretPosition(0);
        filename.setFilename(element.getPropertyAsString(BeanShellSampler.FILENAME));
        parameters.setText(element.getPropertyAsString(BeanShellSampler.PARAMETERS));
        resetInterpreter.setSelected(element.getPropertyAsBoolean(BeanShellSampler.RESET_INTERPRETER));
        super.configure(element);
    }

    @Override
    public TestElement createTestElement() {
        BeanShellSampler sampler = new BeanShellSampler();
        modifyTestElement(sampler);
        return sampler;
    }

    /**
     * Modifies a given TestElement to mirror the data in the gui components.
     *
     * @see org.apache.jmeter.gui.JMeterGUIComponent#modifyTestElement(TestElement)
     */
    @Override
    public void modifyTestElement(TestElement te) {
        te.clear();
        super.configureTestElement(te);
        te.setProperty(BeanShellSampler.SCRIPT, scriptField.getText());
        te.setProperty(BeanShellSampler.FILENAME, filename.getFilename());
        te.setProperty(BeanShellSampler.PARAMETERS, parameters.getText());
        te.setProperty(new BooleanProperty(BeanShellSampler.RESET_INTERPRETER, resetInterpreter.isSelected()));
    }

    /**
     * Implements JMeterGUIComponent.clearGui
     */
    @Override
    public void clearGui() {
        super.clearGui();

        filename.setFilename(""); //$NON-NLS-1$
        parameters.setText(""); //$NON-NLS-1$
        scriptField.setInitialText(""); //$NON-NLS-1$
        resetInterpreter.setSelected(false);
    }

    @Override
    public String getLabelResource() {
        return "bsh_sampler_title";
    }

    private JPanel createFilenamePanel()
    {
        JPanel filenamePanel = new JPanel(new BorderLayout());
        filenamePanel.add(filename, BorderLayout.CENTER);

        return filenamePanel;
    }

    private JPanel createParameterPanel() {
        JLabel label = new JLabel(JMeterUtils.getResString("bsh_script_parameters"));

        parameters = new JTextField(10);
        parameters.setName(BeanShellSampler.PARAMETERS);
        label.setLabelFor(parameters);

        JPanel parameterPanel = new JPanel(new BorderLayout(5, 0));
        parameterPanel.add(label, BorderLayout.WEST);
        parameterPanel.add(parameters, BorderLayout.CENTER);
        return parameterPanel;
    }

    private JPanel createResetPanel() {
        resetInterpreter = new JCheckBox(JMeterUtils.getResString("bsh_script_reset_interpreter"));
        resetInterpreter.setName(BeanShellSampler.PARAMETERS);

        JPanel resetInterpreterPanel = new JPanel(new BorderLayout());
        resetInterpreterPanel.add(resetInterpreter, BorderLayout.WEST);
        return resetInterpreterPanel;
    }

    private void init() { // WARNING: called from ctor so must not be overridden (i.e. must be private or final)
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());

        Box box = Box.createVerticalBox();
        box.add(makeTitlePanel());
        box.add(createResetPanel());
        box.add(createParameterPanel());
        box.add(createFilenamePanel());
        add(box, BorderLayout.NORTH);

        JPanel panel = createScriptPanel();
        add(panel, BorderLayout.CENTER);
        // Don't let the input field shrink too much
        add(Box.createVerticalStrut(panel.getPreferredSize().height), BorderLayout.WEST);
    }

    private JPanel createScriptPanel() {
        scriptField = JSyntaxTextArea.getInstance(20, 20);

        JLabel label = new JLabel(JMeterUtils.getResString("bsh_script"));
        label.setLabelFor(scriptField);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(label, BorderLayout.NORTH);
        panel.add(JTextScrollPane.getInstance(scriptField), BorderLayout.CENTER);

        JTextArea explain = new JTextArea(JMeterUtils.getResString("bsh_script_variables")); //$NON-NLS-1$
        explain.setLineWrap(true);
        explain.setEditable(false);
        explain.setBackground(this.getBackground());
        panel.add(explain, BorderLayout.SOUTH);

        return panel;
    }
}
