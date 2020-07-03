package com.company;

import com.company.utils.SwingUtils;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.*;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.svg.SVGDocument;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.util.Scanner;

public class AppFrame extends JFrame{
    private JTextArea inputGraphTextArea;
    private JButton loadGraphTxt;
    private JButton saveGraphTxt;
    private JButton saveGraphSvg;
    private JButton isEulerButton;
    private JPanel mainPanel;
    private JButton createGraph;
    private JLabel isEulerResultLabel;
    private JPanel panelGraphPainterContainer;

    private JFileChooser fileChooserTxtOpen;
    private JFileChooser fileChooserTxtSave;
    private JFileChooser fileChooserImgSave;

    private Graph graph = null;

    private SvgPanel panelGraphPainter;

    private static class SvgPanel extends JPanel {
        private String svg = null;
        private GraphicsNode svgGraphicsNode = null;

        public void paint(String svg) throws IOException {
            String xmlParser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory df = new SAXSVGDocumentFactory(xmlParser);
            SVGDocument doc = df.createSVGDocument(null, new StringReader(svg));
            UserAgent userAgent = new UserAgentAdapter();
            DocumentLoader loader = new DocumentLoader(userAgent);
            BridgeContext ctx = new BridgeContext(userAgent, loader);
            ctx.setDynamicState(BridgeContext.DYNAMIC);
            GVTBuilder builder = new GVTBuilder();
            svgGraphicsNode = builder.build(ctx, doc);

            this.svg = svg;
            repaint();
        }

        @Override
        public void paintComponent(Graphics gr) {
            super.paintComponent(gr);

            if (svgGraphicsNode == null) {
                return;
            }

            double scaleX = this.getWidth() / svgGraphicsNode.getPrimitiveBounds().getWidth();
            double scaleY = this.getHeight() / svgGraphicsNode.getPrimitiveBounds().getHeight();
            double scale = Math.min(scaleX, scaleY);
            AffineTransform transform = new AffineTransform(scale, 0, 0, scale, 0, 0);
            svgGraphicsNode.setTransform(transform);
            Graphics2D g2d = (Graphics2D) gr;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            svgGraphicsNode.paint(g2d);
        }
    }

    public AppFrame(String title) {
        super(title);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setContentPane(mainPanel);
        this.pack();

        fileChooserTxtOpen = new JFileChooser();
        fileChooserTxtSave = new JFileChooser();
        fileChooserImgSave = new JFileChooser();

        fileChooserTxtOpen.setCurrentDirectory(new File("./input"));
        fileChooserTxtSave.setCurrentDirectory(new File("./output"));
        fileChooserImgSave.setCurrentDirectory(new File("./output"));

        FileFilter txtFilter = new FileNameExtensionFilter("Text files (*.txt)", "txt");
        FileFilter svgFilter = new FileNameExtensionFilter("SVG images (*.svg)", "svg");

        fileChooserTxtOpen.addChoosableFileFilter(txtFilter);
        fileChooserTxtSave.addChoosableFileFilter(txtFilter);
        fileChooserImgSave.addChoosableFileFilter(svgFilter);

        fileChooserTxtSave.setAcceptAllFileFilterUsed(false);
        fileChooserTxtSave.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooserTxtSave.setApproveButtonText("Save");
        fileChooserImgSave.setAcceptAllFileFilterUsed(false);
        fileChooserImgSave.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooserImgSave.setApproveButtonText("Save");

        panelGraphPainterContainer.setLayout(new BorderLayout());
        panelGraphPainter = new SvgPanel();
        panelGraphPainterContainer.add(new JScrollPane(panelGraphPainter));


        loadGraphTxt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (fileChooserTxtOpen.showOpenDialog(AppFrame.this) == JFileChooser.APPROVE_OPTION) {
                    try (Scanner sc = new Scanner(fileChooserTxtOpen.getSelectedFile())) {
                        sc.useDelimiter("\\Z");
                        inputGraphTextArea.setText(sc.next());
                    } catch (Exception exc) {
                        SwingUtils.showErrorMessageBox(exc);
                    }
                }
            }
        });

        saveGraphTxt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (fileChooserTxtSave.showSaveDialog(AppFrame.this) == JFileChooser.APPROVE_OPTION) {
                    String filename = fileChooserTxtSave.getSelectedFile().getPath();
                    if (!filename.toLowerCase().endsWith(".txt")) {
                        filename += ".txt";
                    }
                    try (FileWriter wr = new FileWriter(filename)) {
                        wr.write(inputGraphTextArea.getText());
                        JOptionPane.showMessageDialog(AppFrame.this,
                                "Файл '" + fileChooserTxtSave.getSelectedFile() + "' успешно сохранен");
                    } catch (Exception exc) {
                        SwingUtils.showErrorMessageBox(exc);
                    }
                }
            }
        });

        createGraph.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    String name = "AdjMatrixGraph";
                    Class clz = Class.forName("com.company." + name);
                    Graph graph = GraphUtils.fromStr(inputGraphTextArea.getText(), clz);
                    AppFrame.this.graph = graph;
                    panelGraphPainter.paint(dotToSvg(graph.toDot()));
                } catch (Exception exc) {
                    SwingUtils.showErrorMessageBox(exc);
                }
            }
        });

        saveGraphSvg.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (panelGraphPainter.svg == null) {
                    return;
                }
                if (fileChooserImgSave.showSaveDialog(AppFrame.this) == JFileChooser.APPROVE_OPTION) {
                    String filename = fileChooserImgSave.getSelectedFile().getPath();
                    if (!filename.toLowerCase().endsWith(".svg")) {
                        filename += ".svg";
                    }
                    try (FileWriter wr = new FileWriter(filename)) {
                        wr.write(panelGraphPainter.svg);
                        JOptionPane.showMessageDialog(AppFrame.this,
                                "Файл '" + fileChooserImgSave.getSelectedFile() + "' успешно сохранен");
                    } catch (Exception exc) {
                        SwingUtils.showErrorMessageBox(exc);
                    }
                }
            }
        });

        isEulerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                boolean isEuler = graph.isEuler();
                if (isEuler) {
                    isEulerResultLabel.setText("Эйлеров");
                } else {
                    isEulerResultLabel.setText("Не Эйлеров");
                }
            }
        });
    }

    private static String dotToSvg(String dotSrc) throws IOException {
        MutableGraph g = new Parser().read(dotSrc);
        return Graphviz.fromGraph(g).render(Format.SVG).toString();
    }

}
