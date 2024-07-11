import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

class SeriesData {
    String type;
    int[] data;
    boolean smooth;
    boolean stacked;
    String stack;
    Color color;

    public SeriesData(String type, int[] data, boolean smooth, boolean stacked, String stack, Color color) {
        this.type = type;
        this.data = data;
        this.smooth = smooth;
        this.stacked = stacked;
        this.stack = stack;
        this.color = color;
    }
}

public class ChartPanel extends JPanel {

    private double scaleX = 1.0; // 缩放比例
    private double scaleY = 1.0;

    private boolean showTooltip = false; // 是否显示工具提示
    private String tooltip = ""; // 工具提示内容
    private Point tooltipPosition; // 工具提示框位置，未赋值

    private String[] categories = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    private int[] y0 = {}; // 初始化 y0 数组

    // 边距
    private int padding = 50; // 总
    private int leftPadding = 240; // 左，给文本留位置
    private int rightPadding = 10; // 右
    private int labelPadding = 25; // 标签的

    private double barWidthRatio;// 柱子宽度比例系数

    public double getBarWidthRatio(){
        return barWidthRatio;
    }

    // 定义 SeriesData 类
    private List<SeriesData> seriesDataList = new ArrayList<>();


    // 颜色
    //private Color barColor = new Color(82, 98, 199);
    private Color gridColor = new Color(200, 200, 200);
    //private Color lineColor = new Color(246, 80, 19);
    //private Color[] stackColors = {Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN, Color.PINK};


    // JSON 文本框
    private JTextArea jsonTextArea;

    public ChartPanel() {
        setLayout(new BorderLayout()); // 设置布局为边界布局

        // 创建包含文本框和按钮的面板
        JPanel controlPanel = new JPanel(new BorderLayout());

        // JSON 文本框
        jsonTextArea = new JTextArea(10, 20);
        jsonTextArea.setText(getCurrentDataAsJson().toString(4)); // Pretty print JSON with an indent of 4 spaces
        JScrollPane scrollPane = new JScrollPane(jsonTextArea);
        controlPanel.add(scrollPane, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));

        // 更新数据按钮
        JButton updateButton = new JButton("Update Data");
        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateDataFromJson();
            }
        });
        buttonPanel.add(updateButton);

        // 保存图像按钮
        JButton saveButton = new JButton("Save Image");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveImage();
            }
        });
        buttonPanel.add(saveButton);

        controlPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(controlPanel, BorderLayout.WEST);


        // 添加鼠标移动事件监听器
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateTooltip(e.getPoint());
            }
        });

        // 添加鼠标滚轮事件监听器
        addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isControlDown()) {
                    double scaleChange = e.getPreciseWheelRotation() * 0.1;
                    scaleX += scaleChange;
                    scaleY += scaleChange;
                    repaint();
                }
            }
        });
        barWidthRatio = 0.5;
    }

    //将json文本数据归到seriesDataList中
    //将json文本数据归到seriesDataList中
    private void updateSeriesDataFromJson(JSONObject option) {
        seriesDataList.clear();
        JSONArray seriesArray = option.getJSONArray("series");

        for (int i = 0; i < seriesArray.length(); i++) {
            JSONObject series = seriesArray.getJSONObject(i);
            String type = series.getString("type");
            boolean smooth = series.optBoolean("smooth", false);
            String stack = series.optString("stack", "");
            boolean stacked = !stack.isEmpty();
            JSONArray dataJson = series.getJSONArray("data");
            String colorStr = series.optString("color", "blue");

            Color color;
            //默认是蓝
            switch (colorStr.toLowerCase()) {
                case "red":
                    color = Color.RED;
                    break;
                case "green":
                    color = Color.GREEN;
                    break;
                case "orange":
                    color = Color.ORANGE;
                    break;
                case "magenta":
                    color = Color.MAGENTA;
                    break;
                case "cyan":
                    color = Color.CYAN;
                    break;
                case "pink":
                    color = Color.PINK;
                    break;
                case "blue":
                default:
                    color = Color.BLUE;
                    break;
            }

            int[] data = new int[dataJson.length()];
            for (int j = 0; j < dataJson.length(); j++) {
                data[j] = dataJson.getInt(j);
            }

            seriesDataList.add(new SeriesData(type, data, smooth, stacked, stack, color));
        }
    }


    //总的读json
    private void updateDataFromJson() {
        try {
            //解析JSON对象
            JSONObject jsonObject = new JSONObject(jsonTextArea.getText());
            JSONArray jsonCategories = jsonObject.getJSONObject("xAxis").getJSONArray("data");
            categories = new String[jsonCategories.length()];

            //读标签
            for (int i = 0; i < jsonCategories.length(); i++) {
                categories[i] = jsonCategories.getString(i);
            }

            //读series
            updateSeriesDataFromJson(jsonObject);

            repaint();
        } catch (JSONException ex) {
            JOptionPane.showMessageDialog(this,
                    "Invalid JSON format: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    //初始化
    private JSONObject getCurrentDataAsJson() {
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonData = new JSONArray();
        JSONArray jsonCategories = new JSONArray();
        JSONObject xAxis = new JSONObject();
        JSONObject seriesObject = new JSONObject();
        JSONArray seriesArray = new JSONArray();

        //写origin的series
        seriesDataList.clear();
        String type = new String("bar");
        boolean smooth = false;
        String stack = "";
        boolean stacked = false;
        Color color = Color.BLUE;

        int[] data = {23, 24, 18, 25, 27, 28, 25};
        seriesDataList.add(new SeriesData(type, data, smooth, stacked, stack, color));

        //写origin的json文本

        SeriesData o = seriesDataList.get(0);

        for (int value : o.data) {
            jsonData.put(value);
        }
        for (String category : categories) {
            jsonCategories.put(category);
        }

        try {
            xAxis.put("data", jsonCategories);
            seriesObject.put("type", "bar");
            seriesObject.put("data", jsonData);
            seriesObject.put("smooth", false);
            seriesObject.put("color", "blue");
            seriesArray.put(seriesObject);

            jsonObject.put("xAxis", xAxis);
            jsonObject.put("yAxis", new JSONObject());
            jsonObject.put("series", seriesArray);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }

        return jsonObject;
    }


    private void saveImage() {
        // 创建 BufferedImage 并绘制当前组件
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        paint(g2d);
        g2d.dispose();

        // 打开文件选择器保存图像
        JFileChooser fileChooser = new JFileChooser();
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String fileName = fileToSave.getName();

            // 确保文件名以 ".png" 结尾
            if (!fileName.toLowerCase().endsWith(".png")) {
                fileToSave = new File(fileToSave.getParentFile(), fileName + ".png");
            }

            try {
                ImageIO.write(image, "png", fileToSave);
                System.out.println("Save Successfully, File's Name is: " + fileToSave.getName());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    //获取整个List的最大值
    private int getMaxDataValue() {
        int maxDataValue = 0;

        for (SeriesData series : seriesDataList) {
            if (!series.stacked) {
                for (int value : series.data) {
                    maxDataValue = Math.max(maxDataValue, value);
                }
            } else {
                // Calculate the maximum for stacked data
                int[] cumulativeSums = new int[series.data.length];
                for (SeriesData stackSeries : seriesDataList) {
                    if (series.stack.equals(stackSeries.stack)) {
                        for (int i = 0; i < stackSeries.data.length; i++) {
                            cumulativeSums[i] += stackSeries.data[i];
                        }
                    }
                }
                for (int cumulativeSum : cumulativeSums) {
                    maxDataValue = Math.max(maxDataValue, cumulativeSum);
                }
            }
        }

        return maxDataValue;
    }


    //获得第i的标签所处的x位置
    public int getXX(int i){
        //+=i * barWidth
        int barWidth = (int) ((getWidth() - leftPadding - rightPadding - labelPadding) / (categories.length * scaleX));
        int x = i * barWidth + leftPadding + labelPadding + padding / 2;
        return (int) (x - (getBarWidth() * getBarWidthRatio()) / 2-getGraphics().getFontMetrics().stringWidth(categories[i]));
    }



    //鼠标提示，前面封对象
    private void updateTooltip(Point mousePoint) {
        int barWidth = getBarWidth();
        int actualBarWidth = (int) (barWidth * barWidthRatio);

        for (SeriesData series : seriesDataList) {
            if (series.type.equals("bar")) {
                if (!series.stacked) {
                    for (int i = 0; i < series.data.length; i++) {
                        int barHeight = (int) ((double) series.data[i] / getMaxDataValue() * getPlotAreaHeight());
                        //int x = padding + leftPadding + i * barWidth;
                        //x-=barWidth/2;
                        int x = getXX(i)+barWidth/2-15;
                        int y = getHeight() - padding - barHeight;

                        if (mousePoint.x >= x && mousePoint.x <= (x + actualBarWidth) && mousePoint.y <= getHeight() - padding && mousePoint.y >= (getHeight() - padding - barHeight)) {
                            showTooltip = true;
                            tooltip = String.format("Category: %s, Value: %d", categories[i], series.data[i]);
                            tooltipPosition = mousePoint;
                            repaint();
                            return;
                        }
                    }
                } else {
                    int[] cumulativeSums = new int[series.data.length];
                    for (SeriesData stackSeries : seriesDataList) {
                        if (series.stack.equals(stackSeries.stack)) {
                            for (int i = 0; i < stackSeries.data.length; i++) {
                                cumulativeSums[i] += stackSeries.data[i];
                                int barHeight = (int) ((double) cumulativeSums[i] / getMaxDataValue() * getPlotAreaHeight());
                                //int x = padding + leftPadding + i * barWidth;
                                int x = getXX(i)+barWidth/2;
                                int y = getHeight() - padding - barHeight;

                                if (mousePoint.x >= x && mousePoint.x <= (x + actualBarWidth) && mousePoint.y <= getHeight() - padding && mousePoint.y >= (getHeight() - padding - barHeight)) {
                                    showTooltip = true;
                                    tooltip = String.format("Category: %s, Value: %d", categories[i], stackSeries.data[i]);
                                    tooltipPosition = mousePoint;
                                    repaint();
                                    return;
                                }
                            }
                        }
                    }
                }
            } else if (series.type.equals("line")) {

                for (int i = 0; i < series.data.length; i++) {
                    //int x = padding + leftPadding + i * barWidth + barWidth / 2;
                    int x = getXX(i)+barWidth/2;
                    int y = (int) ((1 - (double) series.data[i] / getMaxDataValue()) * getPlotAreaHeight() + padding);

                    if (mousePoint.distance(x, y) <= 40) {
                        showTooltip = true;
                        tooltip = String.format("Category: %s,Value: %d", categories[i], series.data[i]);
                        tooltipPosition = mousePoint;
                        repaint();
                        return;
                    }

                }
            }
        }

        showTooltip = false;
        repaint();
    }



    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // 开启抗锯齿
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 计算宽度和高度
        int width = getWidth();
        int height = getHeight();
        int barWidth = (int) ((width - leftPadding - rightPadding - labelPadding) / (categories.length * scaleX));

        // 绘制背景
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);


        int maxDataValue = getMaxDataValue();
        // 对最大数据值进行调整，比如最大是28轴线要到30，分割也好
        maxDataValue = ((maxDataValue + 5) / 5) * 5;

        // 绘制网格线
        int numberYDivisions = 10;
        for (int i = 0; i <= numberYDivisions; i++) {
            int x0 = leftPadding + labelPadding;
            int x1 = width - rightPadding;
            int y0 = height - (i * (height - padding * 2 - labelPadding) / numberYDivisions + padding + labelPadding);
            int y1 = y0;

            if (categories.length > 0) {
                g2.setColor(gridColor);
                g2.drawLine(x0, y0, x1, y1);

                g2.setColor(Color.BLACK);
                String yLabel = String.format("%.1f", maxDataValue * (i * 1.0 / numberYDivisions));

                FontMetrics metrics = g2.getFontMetrics();
                int labelWidth = metrics.stringWidth(yLabel);
                g2.drawString(yLabel, leftPadding + labelPadding - labelWidth - 5, y0 + (metrics.getHeight() / 2) - 3);
            }
        }

        //写标签

        int actualBarWidth = (int) (barWidth * barWidthRatio);// 计算实际柱子宽度
        for (int i = 0; i < categories.length; i++) {
            int x = i * barWidth + leftPadding + labelPadding + padding / 2;
            g2.setColor(Color.BLACK);
            g2.drawString(categories[i], x + actualBarWidth / 2 - g2.getFontMetrics().stringWidth(categories[i]) / 2, height - padding + g2.getFontMetrics().getHeight());
        }




        //画轴线
        g2.setColor(Color.BLACK);
        g2.drawLine(leftPadding + labelPadding, height - padding - labelPadding, leftPadding + labelPadding, padding);  // 绘制Y轴
        g2.drawLine(leftPadding+ labelPadding, height - padding - labelPadding, width-rightPadding, height - padding - labelPadding);  // 绘制X轴

        // 绘制图表

        y0 = new int[categories.length]; // 初始化 y0 数组
        int yt = height - padding - labelPadding;
        for (int i = 0; i < categories.length; i++) {
            y0[i] = yt; // x轴位置
        }

        for (int i = 0; i < seriesDataList.size(); i++) {
            SeriesData ser = seriesDataList.get(i);
            if (ser.type.equals("bar")) {
                drawBarChart(g2, width, height, barWidth,maxDataValue,ser,i);
            } else {
                drawLineChart(g2, width, height,barWidth,maxDataValue,ser);
            }

        }

        //调用鼠标提示
        if (showTooltip && tooltipPosition != null) {
            g2.setColor(new Color(255, 255, 150));
            g2.fillRect(tooltipPosition.x, tooltipPosition.y - 20, 200, 20);
            g2.setColor(Color.BLACK);
            g2.drawRect(tooltipPosition.x, tooltipPosition.y - 20, 200, 20);
            g2.drawString(tooltip, tooltipPosition.x + 5, tooltipPosition.y - 5);
        }


    }

    private void drawBarChart(Graphics2D g2, int width, int height, int barWidth,int maxDataValue,SeriesData ser,int stackNum) {
        //绘制柱状图

        double barWidthRatio = 0.5;//柱子宽度比例系数
        int actualBarWidth = (int) (barWidth * barWidthRatio);//计算实际柱子宽度

        if(!ser.stacked) {
            for (int i = 0; i < categories.length; i++) {
                int value = ser.data[i];
                int x = i * barWidth + leftPadding + labelPadding + padding / 2;
                int barHeight = (int) ((double) value / maxDataValue * (height - padding * 2 - labelPadding));
                int y = height - barHeight - padding - labelPadding;
                g2.setColor(ser.color);
                g2.fillRect(x, y, actualBarWidth, barHeight);
            }
        }
        // 如果是堆叠图
        else {
            //int[] y0 = new int[categories.length]; // 初始化 y0 数组

            g2.setColor(ser.color);
            // 遍历堆叠数量
            for (int j = 0; j < categories.length; j++) {
                int value = ser.data[j];
                int x = j * barWidth + leftPadding + labelPadding + padding / 2;
                int barHeight = (int) ((double) value / maxDataValue * (height - padding * 2 - labelPadding));
                int y = y0[j] - barHeight;
                g2.fillRect(x, y, actualBarWidth, barHeight);
                y0[j] = y;
            }



        }

    }

    //图高
    private int getPlotAreaHeight() {
        return getHeight() - padding * 2;
    }

    private void drawLineChart(Graphics2D g2, int width, int height,int barWidth,int maxDataValue,SeriesData ser) {
        // 绘制折线图或平滑曲线图
        int[] xPoints = new int[categories.length];
        int[] yPoints = new int[categories.length];
        //g2.setColor(lineColor);


        double barWidthRatio = 0.5;// 柱子宽度比例系数
        int actualBarWidth = (int) (barWidth * barWidthRatio);// 计算实际柱子宽度
        for (int i = 0; i < categories.length; i++) {
            xPoints[i] = (int) i * barWidth + leftPadding + labelPadding + padding/2;
            xPoints[i]+=+ actualBarWidth / 2 - g2.getFontMetrics().stringWidth(categories[i]) / 2;

            int barHeight = (int) ((double) ser.data[i] / maxDataValue * (height - padding * 2 - labelPadding));
            yPoints[i] = height - barHeight - padding - labelPadding;

            //yPoints[i] = (int) (height - (data[i] * scaleY * (height - padding * 2 - labelPadding) / getMaxDataValue()) - padding - labelPadding);
        }
        g2.setColor(ser.color);
        for (int i = 0; i < categories.length; i++) {
            g2.fillOval(xPoints[i] - 3, yPoints[i] - 3, 6, 6);
        }


        if (ser.smooth) {
            drawSmoothLine(g2, xPoints, yPoints);
        } else {
            g2.drawPolyline(xPoints, yPoints, categories.length);
        }
    }

    private void drawSmoothLine(Graphics2D g2, int[] xPoints, int[] yPoints) {
        if (xPoints.length < 2) {
            return;
        }

        g2.setStroke(new BasicStroke(2.0f));
        //g2.setColor(ser.color);

        // Helper method to interpolate points using Catmull-Rom spline
        BiFunction<Double, double[], Double> interpolate = (t, p) -> {
            double t2 = t * t;
            double t3 = t2 * t;
            return 0.5 * ((2 * p[1]) +
                    (-p[0] + p[2]) * t +
                    (2 * p[0] - 5 * p[1] + 4 * p[2] - p[3]) * t2 +
                    (-p[0] + 3 * p[1] - 3 * p[2] + p[3]) * t3);
        };

        for (int i = 0; i < xPoints.length - 1; i++) {
            double[] px = new double[4];
            double[] py = new double[4];

            // Set control points for Catmull-Rom spline
            if (i == 0) {
                px[0] = xPoints[i];
                py[0] = yPoints[i];
            } else {
                px[0] = xPoints[i - 1];
                py[0] = yPoints[i - 1];
            }

            px[1] = xPoints[i];
            py[1] = yPoints[i];

            px[2] = xPoints[i + 1];
            py[2] = yPoints[i + 1];

            if (i == xPoints.length - 2) {
                px[3] = xPoints[i + 1];
                py[3] = yPoints[i + 1];
            } else {
                px[3] = xPoints[i + 2];
                py[3] = yPoints[i + 2];
            }

            // Draw the spline segment
            for (double t = 0; t < 1; t += 0.01) {
                double x = interpolate.apply(t, px);
                double y = interpolate.apply(t, py);
                double nextX = interpolate.apply(t + 0.01, px);
                double nextY = interpolate.apply(t + 0.01, py);
                //g2.setColor(ser.color);
                g2.draw(new Line2D.Double(x, y, nextX, nextY));
            }
        }
    }


    private int getBarWidth() {
        // 计算柱宽
        return (getWidth() - 2 * padding - labelPadding) / categories.length;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(1200, 600);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Chart Panel");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
