package org.wordpress.android.ui.stats.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class PostViewsModel implements Serializable {
    private String mOriginalResponse;

    private int mHighestMonth, mHighestDayAverage, mHighestWeekAverage;
    private String mDate;
    private VisitModel[] mDayViews; //Used to build the graph
    private List<Year> mYears;
    private List<Year> mAverages;
    private List<Week> mWeeks;

    public String getDate() {
        return mDate;
    }

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

    public List<Year> getAverages() {
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
        mYears = new LinkedList<>();
        mAverages = new LinkedList<>();
        mWeeks = new LinkedList<>();

        JSONArray dataJSON =  response.getJSONArray("data");
        if (dataJSON != null) {
            // Read the position/index of each field in the response
            JSONArray fieldsJSON = response.getJSONArray("fields");
            HashMap<String, Integer> fieldColumnsMapping;
            try {
                fieldColumnsMapping = new HashMap<>(2);
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

            for (String currentYearKey : orderedKeys) {
                Year currentYear = new Year();
                currentYear.setLabel(currentYearKey);

                JSONObject currentYearObj = yearsJSON.getJSONObject(currentYearKey);
                int total = currentYearObj.getInt("total");
                currentYear.setTotal(total);

                JSONObject monthsJSON = currentYearObj.getJSONObject("months");
                Iterator<String> monthsKeys = monthsJSON.keys();
                List<Month> monthsList = new ArrayList<>(monthsJSON.length());
                while (monthsKeys.hasNext()) {
                    String currentMonthKey = monthsKeys.next();
                    int currentMonthVisits = monthsJSON.getInt(currentMonthKey);
                    monthsList.add(new Month(currentMonthKey, currentMonthVisits));
                }

                Collections.sort(monthsList, new Comparator<Month>() {
                    public int compare(Month o1, Month o2) {
                        int v1 = Integer.parseInt(o1.getMonth());
                        int v2 = Integer.parseInt(o2.getMonth());
                        // ascending order
                        return v1 - v2;
                    }
                });

                currentYear.setMonths(monthsList);
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

            for (String currentJSONKey : orderedKeys) {
                Year currentAverage = new Year();
                currentAverage.setLabel(currentJSONKey);

                JSONObject currentAverageJSONObj = averagesJSON.getJSONObject(currentJSONKey);
                currentAverage.setTotal(currentAverageJSONObj.getInt("overall"));

                JSONObject monthsJSON = currentAverageJSONObj.getJSONObject("months");
                Iterator<String> monthsKeys = monthsJSON.keys();
                List<Month> monthsList = new ArrayList<>(monthsJSON.length());
                while (monthsKeys.hasNext()) {
                    String currentMonthKey = monthsKeys.next();
                    int currentMonthVisits = monthsJSON.getInt(currentMonthKey);
                    monthsList.add(new Month(currentMonthKey, currentMonthVisits));
                }
                Collections.sort(monthsList, new java.util.Comparator<Month>() {
                    public int compare(Month o1, Month o2) {
                        int v1 = Integer.parseInt(o1.getMonth());
                        int v2 = Integer.parseInt(o2.getMonth());
                        // ascending order
                        return v1 - v2;
                    }
                });

                currentAverage.setMonths(monthsList);
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
                currentWeek.setAverage(currentWeekJSON.getInt("average"));
                try {
                    if (i == 0 ) {
                        currentWeek.setChange(0);
                    } else {
                        currentWeek.setChange(currentWeekJSON.getInt("change"));
                    }
                } catch (JSONException e){
                    AppLog.w(AppLog.T.STATS, "Cannot parse the change value in weeks section. Trying to understand the meaning: 42!!");
                    //  if i == 0 is the first week. if not it could mean infinity
                    String aProblematicValue = currentWeekJSON.get("change").toString();
                    if (aProblematicValue.contains("infinity")) {
                        currentWeek.setChange(Integer.MAX_VALUE);
                    } else {
                        currentWeek.setChange(0);
                    }
                }

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
        private int mCount;
        private String mDay;

        public String getDay() {
            return mDay;
        }

        public void setDay(String day) {
            this.mDay = day;
        }

        public int getCount() {
            return mCount;
        }

        public void setCount(int count) {
            this.mCount = count;
        }
    }

    public class Week implements Serializable {
        int mChange;
        int mTotal;
        int mAverage;
        List<Day> mDays = new LinkedList<>();

        public int getTotal() {
            return mTotal;
        }

        public void setTotal(int total) {
            this.mTotal = total;
        }

        public int getAverage() {
            return mAverage;
        }

        public void setAverage(int average) {
            this.mAverage = average;
        }

        public int getChange() {
            return mChange;
        }

        public void setChange(int change) {
            this.mChange = change;
        }

        public List<Day> getDays() {
            return mDays;
        }

        public void setDays(List<Day> days) {
            this.mDays = days;
        }
    }

    public class Year implements Serializable {
        private String mLabel;
        private int mTotal;
        private List<Month> mMonths;

        public List<Month> getMonths() {
            return mMonths;
        }

        public void setMonths(List<Month> months) {
            mMonths = months;
        }

        public String getLabel() {
            return mLabel;
        }

        public void setLabel(String label) {
            this.mLabel = label;
        }

        public int getTotal() {
            return mTotal;
        }

        public void setTotal(int total) {
            this.mTotal = total;
        }
    }

    public class Month implements Serializable {
        private final int mCount;
        private final String mMonth;

        Month(String label, int count) {
            this.mMonth = label;
            this.mCount = count;
        }

        public String getMonth() {
            return mMonth;
        }
        public int getCount() {
            return mCount;
        }
    }
}