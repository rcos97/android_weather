package com.example.weather;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.weather.gson.Forecast;
import com.example.weather.gson.Weather;
import com.example.weather.util.HttpUtil;
import com.example.weather.util.Utility;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.text.CollationElementIterator;
import java.util.List;

import interfaces.heweather.com.interfacesmodule.bean.air.Air;
import interfaces.heweather.com.interfacesmodule.bean.air.now.AirNow;
import interfaces.heweather.com.interfacesmodule.bean.weather.lifestyle.Lifestyle;
import interfaces.heweather.com.interfacesmodule.bean.weather.now.Now;
import interfaces.heweather.com.interfacesmodule.view.HeConfig;
import interfaces.heweather.com.interfacesmodule.view.HeWeather;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    private ScrollView weatherLayout;
    public String mWeatherId;

    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;

    private ImageView bingPicImg;

    public SwipeRefreshLayout swipeRefresh;

    public DrawerLayout drawerLayout;
    private Button navButton;

    private static final String TAG = "WeatherActivity";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HeConfig.init("HE2004292017541020", "dbea5720402e4eea9a5d2bdbbc244c65");
        HeConfig.switchToFreeServerNode();

        if(Build.VERSION.SDK_INT>=21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);//布局显示到状态栏上
            getWindow().setStatusBarColor(Color.TRANSPARENT);//状态栏透明
        }

        setContentView(R.layout.activity_weather);


        swipeRefresh = findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        //初始化各个控件

        weatherLayout = findViewById(R.id.weather_layout);
        titleCity = findViewById(R.id.title_city);
        titleUpdateTime = findViewById(R.id.title_update_time);
        degreeText = findViewById(R.id.degree_text);
        weatherInfoText = findViewById(R.id.weather_info_text);
        forecastLayout = findViewById(R.id.forecast_layout);
        aqiText = findViewById(R.id.aqi_text);
        pm25Text = findViewById(R.id.pm25_text);
        comfortText = findViewById(R.id.comfort_text);
        carWashText = findViewById(R.id.car_wash_text);
        sportText = findViewById(R.id.sport_text);

        //图片

        bingPicImg = findViewById(R.id.bing_pic_img);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//        String weatherString = prefs.getString("weather",null);//weather存的是天气


//        if(weatherString != null){
//            //有缓存直接解析数据
//            Weather weather = Utility.handleWeatherResponse(weatherString);
//            mWeatherId = weather.basic.weatherId;
//            showWeatherInfo(weather);
//        }else{
            mWeatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
//        }
//        Log.d(TAG, "onCreate: sting"+weatherString);
        Log.d(TAG, "onCreate: id"+mWeatherId);
        //图片
        String bingPic = prefs.getString("bing_pic",null);
        if(bingPic!=null){
            Glide.with(this).load(bingPic).into(bingPicImg);//图片加载
        }else{
            loadingBingPic();
        }

        //下拉刷新

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });

        //左菜单

        drawerLayout=findViewById(R.id.drawer_layout);
        navButton = findViewById(R.id.nav_button);
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }
    private void loadingBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }


    //根据 id 获取真实天气
    public void requestWeather(final String weatherId){
        Log.d(TAG, "requestWeather: "+weatherId);
        final Weather weather = new Weather();
        //基本数据
        HeWeather.getWeatherNow(WeatherActivity.this, weatherId, new HeWeather.OnResultWeatherNowBeanListener() {
            @Override
            public void onError(Throwable throwable) {
                Log.d(TAG, "onError: basic");
                Toast.makeText(WeatherActivity.this,"获取天气失败",Toast.LENGTH_SHORT).show();
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onSuccess(Now now) {
                weather.basic.cityName=now.getBasic().getLocation();
                weather.basic.update.updateTime =now.getUpdate().getLoc();
                weather.basic.weatherId = weatherId;
                Log.d(TAG, "onSuccess: "+weather.basic.cityName);
                //未来几天的天气预报
                HeWeather.getWeatherForecast(WeatherActivity.this, weatherId, new HeWeather.OnResultWeatherForecastBeanListener() {
                    @Override
                    public void onError(Throwable throwable) {
                        Toast.makeText(WeatherActivity.this,"获取天气失败",Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                        Log.d(TAG, "onError: forcast");
                    }

                    @Override
                    public void onSuccess(interfaces.heweather.com.interfacesmodule.bean.weather.forecast.Forecast forecast) {
                        for(int i = 0; i < forecast.getDaily_forecast().size();i++){
                            Forecast forecastTemp = new Forecast();
                            forecastTemp.date = forecast.getDaily_forecast().get(i).getDate();
                            forecastTemp.temperature.max=forecast.getDaily_forecast().get(i).getTmp_max();
                            forecastTemp.temperature.min = forecast.getDaily_forecast().get(i).getTmp_min();
                            forecastTemp.more.info = forecast.getDaily_forecast().get(i).getCond_code_d();
                            weather.forecasts.add(forecastTemp);
                        }
                        //AQI 指数
                        HeWeather.getAirNow(WeatherActivity.this, weatherId, new HeWeather.OnResultAirNowBeansListener() {
                            @Override
                            public void onError(Throwable throwable) {
                                Toast.makeText(WeatherActivity.this,"获取天气失败",Toast.LENGTH_SHORT).show();
                                swipeRefresh.setRefreshing(false);
                                Log.d(TAG, "onError: getAirNow");
                            }

                            @Override
                            public void onSuccess(AirNow airNow) {
                                if(airNow.getAir_now_city()!=null) {
                                    Log.d(TAG, "onSuccess:air "+airNow.getAir_now_city().getAqi());
                                    weather.aqi.city.aqi = airNow.getAir_now_city().getAqi();
                                    weather.aqi.city.pm25 = airNow.getAir_now_city().getPm25();
                                }else{
                                    weather.aqi.city.aqi = "无数据";
                                    weather.aqi.city.pm25 = "无数据";
                                }
                                //获取实况天气
                                HeWeather.getWeatherNow(WeatherActivity.this, weatherId, new HeWeather.OnResultWeatherNowBeanListener() {
                                    @Override
                                    public void onError(Throwable throwable) {
                                        Toast.makeText(WeatherActivity.this,"获取天气失败",Toast.LENGTH_SHORT).show();
                                        swipeRefresh.setRefreshing(false);
                                        Log.d(TAG, "onError: now");
                                    }

                                    @Override
                                    public void onSuccess(Now now) {
                                        Log.d(TAG, "onSuccess: "+now.getNow().getTmp());
                                        weather.now.temperature=now.getNow().getTmp();
                                        weather.now.more.Info=now.getNow().getCond_txt();
                                        //获取建议
                                        HeWeather.getWeatherLifeStyle(WeatherActivity.this, "CN101010100", new HeWeather.OnResultWeatherLifeStyleBeanListener() {
                                            @Override
                                            public void onError(Throwable throwable) {
                                                Toast.makeText(WeatherActivity.this,"获取天气失败",Toast.LENGTH_SHORT).show();
                                                swipeRefresh.setRefreshing(false);
                                            }
                                            @Override
                                            public void onSuccess(Lifestyle lifestyle) {
                                                for(int i = 0; i < lifestyle.getLifestyle().size(); i++){
                                                    if(lifestyle.getLifestyle().get(i).getType().equals("comf")){
                                                        weather.suggestion.comfort.info=lifestyle.getLifestyle().get(i).getTxt();
                                                    }else if(lifestyle.getLifestyle().get(i).getType().equals("cw")){
                                                        weather.suggestion.carWash.info = lifestyle.getLifestyle().get(i).getTxt();
                                                    }else if(lifestyle.getLifestyle().get(i).getType().equals("sport")){
                                                        weather.suggestion.sport.info = lifestyle.getLifestyle().get(i).getTxt();
                                                    }
                                                }
                                                //绘制界面
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
//                                                        if(weather != null &&"ok".equals(weather.status)){
//                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
//                            editor.putString("weather",responseText);
//                            editor.apply();
//                                                            showWeatherInfo(weather);
//                                                        }else{
//                                                            Toast.makeText(WeatherActivity.this,"获取天气失败",Toast.LENGTH_SHORT).show();
//                                                        }
                                                        showWeatherInfo(weather);
                                                        swipeRefresh.setRefreshing(false);
                                                    }
                                                });
                                                loadingBingPic();
                                            }
                                        });

                                    }
                                });
                            }
                        });
                    }


                });
            }
        });

    }

    // 根据天气id获得天气信息
//    public void requestWeather(final String weatherId){
//        String weatherUrl = "http://guolin.tech/api/weather?cityid="+weatherId+"&key=d8c67448f18343549de0c909fc0fb2f8";
//        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(WeatherActivity.this,"获取天气失败",Toast.LENGTH_SHORT).show();
//                        swipeRefresh.setRefreshing(false);
//                    }
//                });
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                final String responseText = response.body().string();
//                final Weather weather = Utility.handleWeatherResponse(responseText);
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if(weather != null &&"ok".equals(weather.status)){
//                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
//                            editor.putString("weather",responseText);
//                            editor.apply();
//                            showWeatherInfo(weather);
//                        }else{
//                            Toast.makeText(WeatherActivity.this,"获取天气失败",Toast.LENGTH_SHORT).show();
//                        }
//                        swipeRefresh.setRefreshing(false);
//                    }
//                });
//            }
//        });
//        loadingBingPic();
//    }

    private  void showWeatherInfo(Weather weather){
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime;
        String degree = weather.now.temperature+"度";
        String weatherInfo = weather.now.more.Info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for(Forecast forecast : weather.forecasts){
            Log.d(TAG, "showWeatherInfo: "+forecast.date);
            View view = LayoutInflater.from(this).inflate(R.layout.forcast_item,forecastLayout,false);
            TextView dateText = view.findViewById(R.id.date_txt);
            TextView infoText = view.findViewById(R.id.info_txt);
            TextView maxText = view.findViewById(R.id.max_txt);
            TextView minText = view.findViewById(R.id.min_txt);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if(weather.aqi!=null){
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }

        String comfort = "舒适度:\n"+weather.suggestion.comfort.info;
        String carWash = "洗车指数:\n"+weather.suggestion.carWash.info;
        String sport = "运动建议:\n" + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
    }


//    //获取Basic
//    public void requestBasic(final String weatherId){
//        String BasicUrl = "https://free-api.heweather.net/s6/weather/now?location=CN101010100"+"&key=dbea5720402e4eea9a5d2bdbbc244c65";
//        HttpUtil.sendOkHttpRequest(BasicUrl, new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(WeatherActivity.this,"获取天气失败",Toast.LENGTH_SHORT).show();
//                        swipeRefresh.setRefreshing(false);
//                    }
//                });
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                final String responseText = response.body().string();
//                Log.d(TAG, "onResponse: basic"+responseText);
//            }
//        });
//    }
}
