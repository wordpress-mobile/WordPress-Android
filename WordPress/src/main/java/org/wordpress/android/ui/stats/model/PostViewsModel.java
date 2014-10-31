package org.wordpress.android.ui.stats.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class PostViewsModel implements Serializable {
    private String mOriginalResponse;

    private int mHighestMonth, mHighestDayAverage, mHighestWeekAverage;
    private String mDate;
    private VisitModel[] mDayViews; //Used to build the graph
    private HashMap<String, Integer> fieldColumnsMapping;
    private List<Year> mYears;
    private List<Average> mAverages;
    private List<Week> mWeeks;

    public String getOriginalResponse() {
        return mOriginalResponse;
    }

    public VisitModel[] getDayViews() {
        return mDayViews;
    }

    public int getHighestMonth() {
        return mHighestMonth;
    }

    public int getHighestDayAverage() {
        return mHighestDayAverage;
    }

    public int getHighestWeekAverage() {
        return mHighestWeekAverage;
    }

    public List<Year> getYears() {
        return mYears;
    }

    public List<Average> getAverages() {
        return mAverages;
    }

    public List<Week> getWeeks() {
        return mWeeks;
    }


    public PostViewsModel(String response) throws JSONException {
            this.mOriginalResponse = response;
            JSONObject responseObj = new JSONObject(response);
            parseResponseObject(responseObj);
    }

    public PostViewsModel(JSONObject response) throws JSONException  {
        if (response == null) {
            return;
        }
        this.mOriginalResponse = response.toString();
        parseResponseObject(response);
    }

    private void parseResponseObject(JSONObject response) throws JSONException  {

        mDate = response.getString("date");
        mHighestDayAverage = response.getInt("highest_day_average");
        mHighestWeekAverage = response.getInt("highest_week_average");
        mHighestMonth = response.getInt("highest_month");
        mYears = new LinkedList<Year>();
        mAverages = new LinkedList<Average>();
        mWeeks = new LinkedList<Week>();

        JSONArray dataJSON =  response.getJSONArray("data");
        if (dataJSON != null) {
            // Read the position/index of each field in the response
            JSONArray fieldsJSON = response.getJSONArray("fields");
            try {
                fieldColumnsMapping = new HashMap<String, Integer>(2);
                for (int i = 0; i < fieldsJSON.length(); i++) {
                    final String field = fieldsJSON.getString(i);
                    fieldColumnsMapping.put(field, i);
                }
            } catch (JSONException e) {
                AppLog.e(AppLog.T.STATS, "Cannot read the fields indexes from the JSON response", e);
                throw e;
            }

            VisitModel[] visitModels = new VisitModel[dataJSON.length()];
            int viewsColumnIndex = fieldColumnsMapping.get("views");
            int periodColumnIndex = fieldColumnsMapping.get("period");

            for (int i = 0; i < dataJSON.length(); i++) {
                try {
                    JSONArray currentDayData = dataJSON.getJSONArray(i);
                    VisitModel currentVisitModel = new VisitModel();
                    currentVisitModel.setPeriod(currentDayData.getString(periodColumnIndex));
                    currentVisitModel.setViews(currentDayData.getInt(viewsColumnIndex));
                    visitModels[i] = currentVisitModel;
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.STATS, "Cannot create the Visit at index " + i, e);
                }
            }
            mDayViews = visitModels;
        } else {
            mDayViews = null;
        }

        parseYears(response);
        parseAverages(response);
        parseWeeks(response);
    }

    private String[] orderKeys(Iterator keys, int numberOfKeys) {
        // Keys could not be ordered fine. Reordering them.
        String[] orderedKeys = new String[numberOfKeys];
        int i = 0;
        while (keys.hasNext()) {
            orderedKeys[i] = (String)keys.next();
            i++;
        }
        Arrays.sort(orderedKeys);
        return orderedKeys;
    }

    private void parseYears(JSONObject response) {
        // Parse the Years section
        try {
            JSONObject yearsJSON = response.getJSONObject("years");
            // Keys could not be ordered fine. Reordering them.
            String[] orderedKeys = orderKeys(yearsJSON.keys(), yearsJSON.length());

            for (int j = 0; j < orderedKeys.length; j++) {
                Year currentYear = new Year();
                String currentYearKey = orderedKeys[j];
                currentYear.setLabel(currentYearKey);

                JSONObject currentYearObj = yearsJSON.getJSONObject(currentYearKey);
                int total = currentYearObj.getInt("total");
                currentYear.setTotal(total);

                JSONObject months = currentYearObj.getJSONObject("months");
                String[] orderedMonthsKeys = orderKeys(months.keys(), months.length());
                for (int i = 0; i < orderedMonthsKeys.length; i++) {
                    String currentMonthKey = orderedMonthsKeys[i];
                    int currentMonthVisits = months.getInt(currentMonthKey);
                    int currentMonthIndex = Integer.parseInt(currentMonthKey) - 1;
                    currentYear.getMonths()[currentMonthIndex] = currentMonthVisits;
                }
                mYears.add(currentYear);
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, "Cannot parse the Years section", e);
        }
    }

    private void parseAverages(JSONObject response) {
        // Parse the Averages section
        try {
            JSONObject averagesJSON = response.getJSONObject("averages");
            // Keys could not be ordered fine. Reordering them.
            String[] orderedKeys = orderKeys(averagesJSON.keys(), averagesJSON.length());

            for (int j = 0; j < orderedKeys.length; j++) {
                Average currentAverage = new Average();
                String currentJSONKey = orderedKeys[j];
                currentAverage.setLabel(currentJSONKey);

                JSONObject currentAverageJSONObj = averagesJSON.getJSONObject(currentJSONKey);
                currentAverage.setOverall(currentAverageJSONObj.getInt("overall"));

                JSONObject monthsJSON = currentAverageJSONObj.getJSONObject("months");
                String[] orderedMonthsKeys = orderKeys(monthsJSON.keys(), monthsJSON.length());
                for (int i = 0; i < orderedMonthsKeys.length; i++) {
                    String currentMonthKey = orderedMonthsKeys[i];
                    int currentMonthVisits = monthsJSON.getInt(currentMonthKey);
                    int currentMonthIndex = Integer.parseInt(currentMonthKey) - 1;
                    currentAverage.getMonths()[currentMonthIndex] = currentMonthVisits;
                }
                mAverages.add(currentAverage);
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, "Cannot parse the Averages section", e);
        }
    }

    private void parseWeeks(JSONObject response) {
        // Parse the Weeks section
        try {
            JSONArray weeksJSON = response.getJSONArray("weeks");
            for (int i = 0; i < weeksJSON.length(); i++) {
                Week currentWeek = new Week();
                JSONObject currentWeekJSON = weeksJSON.getJSONObject(i);

                currentWeek.setTotal(currentWeekJSON.getInt("total"));

                try {
                    if (i == 0 ) {
                        currentWeek.setChange(0);
                    } else {
                        currentWeek.setChange(currentWeekJSON.getInt("change"));
                    }
                } catch (JSONException e){
                    AppLog.w(AppLog.T.STATS, "Cannot parse the change value in weeks section. Trying to understand the meaning: 42!!");
                    //  if i == 0 is the first week. if notit could mean infinity
                    String aProblematicValue = currentWeekJSON.get("change").toString();
                    if (aProblematicValue.contains("infinity")) {
                        currentWeek.setChange(Integer.MAX_VALUE);
                    } else {
                        currentWeek.setChange(0);
                    }
                }

                currentWeek.setAverage(currentWeekJSON.getInt("average"));

                JSONArray daysJSON = currentWeekJSON.getJSONArray("days");
                for (int j = 0; j < daysJSON.length(); j++) {
                    Day currentDay = new Day();
                    JSONObject dayJSON = daysJSON.getJSONObject(j);
                    currentDay.setCount(dayJSON.getInt("count"));
                    currentDay.setDay(dayJSON.getString("day"));
                    currentWeek.getDays().add(currentDay);
                }
                mWeeks.add(currentWeek);
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, "Cannot parse the Weeks section", e);
        }
    }

    public class Day implements Serializable {
        private int count;
        private String day;

        public String getDay() {
            return day;
        }

        public void setDay(String day) {
            this.day = day;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }

    public class Week implements Serializable {
        int change;
        int total;
        int average;
        List<Day> days = new LinkedList<Day>();

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getAverage() {
            return average;
        }

        public void setAverage(int average) {
            this.average = average;
        }

        public int getChange() {
            return change;
        }

        public void setChange(int change) {
            this.change = change;
        }

        public List<Day> getDays() {
            return days;
        }

        public void setDays(List<Day> days) {
            this.days = days;
        }
    }

    public class Average implements Serializable {
        private String label;
        private int overall;
        private int[] months = new int[12];

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public int getOverall() {
            return overall;
        }

        public void setOverall(int overall) {
            this.overall = overall;
        }

        public int[] getMonths() {
            return months;
        }

        public void setMonths(int[] months) {
            this.months = months;
        }
    }

    public class Year implements Serializable {
        String label;
        int total;
        int[] months = new int[12];

        public int[] getMonths() {
            return months;
        }

        public void setMonths(int[] months) {
            this.months = months;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }
    }
}