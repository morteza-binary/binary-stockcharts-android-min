package com.binary.binarystockchart.charts;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.binary.binarystockchart.R;
import com.binary.binarystockchart.data.TickEntry;
import com.binary.binarystockchart.formatter.DateTimeAxisFormatter;
import com.binary.binarystockchart.interfaces.indecators.IIndicator;
import com.binary.binarystockchart.utils.ColorUtils;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.HighlightArea;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by morteza on 12/18/2017.
 */

public class BinaryLineChart extends CombinedChart implements OnChartGestureListener {

    private Boolean plotLineEnabled = true;
    private Boolean autoScrollingEnabled = true;
    private Boolean drawCircle = false;
    private Long epochReference = 0L;
    private Integer defaultXAxisZoom = 20;
    private Integer defaultYAxixZoom = 1;
    private LimitLine plotLine;
    private LimitLine startSpotLine;
    private LimitLine entrySpotLine;
    private LimitLine exitSpotLine;
    private List<LimitLine> barrierLines = new ArrayList<>();
    private HighlightArea purchaseHighlightArea;
    private ChartType mainChartType = ChartType.LINE;
    private List<IIndicator> indicators = new ArrayList<>();

    public BinaryLineChart(Context context) {
        super(context);
    }

    public BinaryLineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BinaryLineChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public enum ChartType {
        LINE, CANDLE
    }

    public enum DataSetLabels {
        MAIN("MAIN");

        private final String value;

        DataSetLabels(String _value) {
            value = _value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    @Override
    public void init() {
        super.init();
        this.getDescription().setEnabled(false);
        this.getLegend().setEnabled(false);
        this.setOnChartGestureListener(this);
        configYAxis();
        configXAxis();
    }

    private CombinedData generateCombinedData() {
        CombinedData combinedData = this.getData();

        if(combinedData == null) {
            combinedData = new CombinedData();
        }
        return combinedData;
    }

    private LineData generateLineData() {
        CombinedData combinedData = this.generateCombinedData();

        LineData lineData = combinedData.getLineData();

        if(lineData == null) {
            lineData = new LineData();
            combinedData.setData(lineData);
        }

        ILineDataSet lineDataSet = lineData.getDataSetByLabel(
                DataSetLabels.MAIN.toString(),
                false
        );

        if(lineDataSet == null) {
            lineDataSet = createSet();
            lineData.addDataSet(lineDataSet);

            this.setData(combinedData);
        }

        return lineData;
    }

    private ILineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, DataSetLabels.MAIN.toString());

        set.setColor(ColorUtils.getColor(getContext(), R.color.colorLineChart));
        set.setCircleColor(ColorUtils.getColor(getContext(), R.color.colorLineChartCircle));
        set.setCircleColorHole(ColorUtils.getColor(getContext(), R.color.colorLineChartCircle));
        set.setHighLightColor(ColorUtils.getColor(getContext(), R.color.colorCrossHair));
        set.setValueTextColor(ColorUtils.getColor(getContext(), R.color.colorLineChartValue));
        set.setFillColor(ColorUtils.getColor(getContext(), R.color.colorLineChartFill));

        set.setDrawCircles(this.drawCircle);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // fill drawable only supported on api level 18 and above
            Drawable drawable = getResources().getDrawable(R.drawable.fade_blue);
            set.setFillDrawable(drawable);
        } else {
            set.setFillColor(Color.BLACK);
        }

        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setHighlightEnabled(true);
        set.setValueTextSize(8f);
        set.setDrawValues(false);
        set.setDrawFilled(true);

        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        return set;
    }

    public void addTick(TickEntry tick) {

        LineData lineData = generateLineData();


        if(this.epochReference == 0L) {
            this.epochReference = tick.getEpoch();
        }

        lineData.addEntry(
                new Entry(tick.getEpoch() - this.epochReference, tick.getQuote()),
                0
        );

        this.handlesIndicators();
        this.getCombinedData().notifyDataChanged();
        this.setXAxisMax(tick.getEpoch() - this.epochReference);

        if (this.plotLineEnabled) {
            this.updatePlotLine(tick.getQuote());
        }

        if (this.autoScrollingEnabled) {
            this.moveViewToX(tick.getEpoch() - this.epochReference);
        }
    }

    public void addTicks(List<TickEntry> ticks) {

        LineData lineData = generateLineData();

        if (this.epochReference == 0L) {
            this.epochReference = ticks.get(0).getEpoch();
        }

        for (TickEntry tick : ticks) {
            lineData.addEntry(new Entry(tick.getEpoch() - this.epochReference, tick.getQuote()),
                    "MAIN");
        }

        this.getCombinedData().notifyDataChanged();

        this.handlesIndicators();
        this.notifyDataSetChanged();


        TickEntry lastTick = Iterables.getLast(ticks);
        if (this.plotLineEnabled) {
            this.updatePlotLine(lastTick.getQuote());
        }

        this.setXAxisMax(lastTick.getEpoch() - this.epochReference);

        this.zoom(this.defaultXAxisZoom, defaultYAxixZoom, 0, 0);

        if (this.autoScrollingEnabled) {
            this.moveViewToX(Iterables.getLast(ticks).getEpoch() - this.epochReference);
        }
    }

    private void handlesIndicators() {
        LineData chartData = generateLineData();

        for(IIndicator indicator : this.indicators) {
            if(indicator.getChartData() == null) {
                indicator.setChartData(this.getCombinedData());
            }

            indicator.notifyDataChanged();
        }
    }

    public void addEntrySpot(TickEntry tick) {
        this.entrySpotLine = new LimitLine(tick.getEpoch() - this.epochReference);
        this.entrySpotLine.setLineColor(ColorUtils.getColor(getContext(), R.color.colorEntrySpotLit));
        this.entrySpotLine.setLineWidth(2f);
        this.getXAxis().addLimitLine(this.entrySpotLine);
        this.invalidate();
    }

    public void addStartSpot(TickEntry tick) {
        this.startSpotLine = new LimitLine(tick.getEpoch() - this.epochReference);
        this.startSpotLine.setLineColor(ColorUtils.getColor(getContext(), R.color.colorStartSpotLine));
        this.startSpotLine.setLineWidth(2f);
        this.getXAxis().removeAllLimitLines();
        this.getXAxis().removeHighlightArea(purchaseHighlightArea);
        this.entrySpotLine = null;
        this.exitSpotLine = null;
        this.getXAxis().addLimitLine(this.startSpotLine);
        this.invalidate();
    }

    public void addExitSpot(TickEntry tick) {
        this.exitSpotLine = new LimitLine(tick.getEpoch() - this.epochReference);
        this.exitSpotLine.setLineWidth(2f);
        this.exitSpotLine.setLineColor(ColorUtils.getColor(getContext(), R.color.colorExitSpotLit));
        this.getXAxis().addLimitLine(this.exitSpotLine);
        this.invalidate();
    }

    public void addHighlightArea(TickEntry tick, int areaColor) {
        float endPoint = tick.getEpoch() - this.epochReference;

        if (this.exitSpotLine != null) {
            endPoint = exitSpotLine.getLimit();
        }

        if (this.purchaseHighlightArea != null) {
            this.getXAxis().removeHighlightArea(this.purchaseHighlightArea);
        }
        this.purchaseHighlightArea = new HighlightArea(this.entrySpotLine.getLimit(), endPoint);
        this.purchaseHighlightArea.setAreaColor(areaColor);
        this.getXAxis().addHighlightArea(this.purchaseHighlightArea);
        this.invalidate();

    }

    public void addBarrierLine(final Float barrierValue, final String label) {
        LimitLine barrierLine = new LimitLine(barrierValue, label);
        barrierLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        barrierLine.enableDashedLine(30f, 10f, 0);
        barrierLine.setLineColor(ColorUtils.getColor(getContext(), R.color.colorBarrierLine));
        barrierLine.setTextColor(ColorUtils.getColor(getContext(), R.color.colorBarrierText));

        barrierLine.setLabelBackground(LimitLine.LimitLineLabelBackground.RECTANGLE);
        barrierLine.setLabelBackgroundStyle(Paint.Style.STROKE);
        barrierLine.setLabelBackgroundColor(ColorUtils.getColor(getContext(),
                R.color.colorBarrierBg));

        this.barrierLines.add(barrierLine);
        this.getAxisLeft().addLimitLine(barrierLine);
        this.invalidate();
    }

    public void addBarrierLine(final Float barrierValue) {
        this.addBarrierLine(barrierValue, barrierValue.toString());
    }

    public void removeAllBarrierLines() {
        for (LimitLine limitLine : this.barrierLines) {
            this.getAxisLeft().removeLimitLine(limitLine);
        }
        this.invalidate();
    }

    private void updatePlotLine(Float value) {
        if (plotLine != null) {
            this.getAxisLeft().removeLimitLine(plotLine);
        }
        this.plotLine = new LimitLine(value, value.toString());
        this.plotLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);

        this.plotLine.setLineColor(ColorUtils.getColor(getContext(), R.color.colorPlotLine));
        this.plotLine.setTextColor(ColorUtils.getColor(getContext(), R.color.colorPlotText));

        this.plotLine.setLabelBackground(LimitLine.LimitLineLabelBackground.POLYGON);
        this.plotLine.setLabelBackgroundColor(ColorUtils.getColor(getContext(),
                R.color.colorPlotBg));

        this.getAxisLeft().addLimitLine(plotLine);
        this.invalidate();
    }

    private void setXAxisMax(float x) {
        mXAxis.setAxisMaximum(x + this.getVisibleXRange() / 3);
    }

    private void configXAxis() {
        XAxis xAxis = this.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new DateTimeAxisFormatter(this));
        xAxis.setLabelCount(5);
//        xAxis.setAxisMinimum(0f);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
    }

    private void configYAxis() {
        YAxis yAxis = this.getAxisRight();
        yAxis.setEnabled(false);
    }

    public Long getEpochReference() {
        return epochReference;
    }

    public void setEpochReference(Long epochReference) {
        this.epochReference = epochReference;
    }

    public Boolean getPlotLineEnabled() {
        return plotLineEnabled;
    }

    public void setPlotLineEnabled(Boolean plotLineEnabled) {
        this.plotLineEnabled = plotLineEnabled;
    }

    public Boolean getDrawCircle() {
        return drawCircle;
    }

    public Integer getDefaultXAxisZoom() {
        return defaultXAxisZoom;
    }

    public void setDefaultXAxisZoom(Integer defaultXAxisZoom) {
        this.defaultXAxisZoom = defaultXAxisZoom;
    }

    public Integer getDefaultYAxixZoom() {
        return defaultYAxixZoom;
    }

    public void setDefaultYAxixZoom(Integer defaultYAxixZoom) {
        this.defaultYAxixZoom = defaultYAxixZoom;
    }

    public void setDrawCircle(Boolean drawCircle) {
        this.drawCircle = drawCircle;

        List<ILineDataSet> sets = this.generateLineData().getDataSets();

        for (ILineDataSet iSet : sets) {
            LineDataSet set = (LineDataSet) iSet;
            set.setDrawCircles(this.drawCircle);
        }
        this.invalidate();
    }

    public void addIndicator(IIndicator indicator) {
        this.indicators.add(indicator);
    }

    public void removeIndicator(IIndicator indicator) {
        this.indicators.remove(indicator);
    }

    public void removeIndicator(int index) {
        this.indicators.remove(index);
    }

    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        float result = Math.abs(Math.round(this.getHighestVisibleX()) - this.getXAxis().getAxisMaximum());
        if (result < 1 && result >= 0) {
            this.autoScrollingEnabled = true;
        } else {
            this.autoScrollingEnabled = false;
        }
    }

    @Override
    public void onChartLongPressed(MotionEvent me) {

    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {

    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {

    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
        if ( this.autoScrollingEnabled) {
            this.moveViewToX(this.getHighestVisibleX());
        }
    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {

    }
}
