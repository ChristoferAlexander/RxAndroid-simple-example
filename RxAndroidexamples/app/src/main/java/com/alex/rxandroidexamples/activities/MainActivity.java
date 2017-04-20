package com.alex.rxandroidexamples.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.alex.rxandroidexamples.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    TextView textViewWeather;
    TextView textViewWeatherInterval;
    TextView textViewWeatherDouble;

    Button buttonFetchWeather;
    Button buttonFetchIntervalWeather;
    Button buttonFetchDoubleWeather;
    //HTTP client to use for requests
    OkHttpClient client = new OkHttpClient();
    //Here we keep all subscriptions to dispose onPause() to avoid any leaks
    private final CompositeDisposable disposables = new CompositeDisposable();

    //Observable that emits weather as a string to subscribed Observers after making a background API call to fetch it
    Observable<String> fetchWeather = Observable.fromCallable(new Callable<String>() {
        @Override
        public String call() throws Exception {
            return getWeather("http://samples.openweathermap.org/data/2.5/weather?", "London,uk", "b1b15e88fa797225412429c1c50c122a1");
        }
    })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    /*Observer that listens above Observable for emitted items and displays them on MainThread
    We add this Observer to disposables to dispose onPause()
    We handle all errors from subscribed Observables inside Observer as well*/
    Observer<String> displayWeather = new Observer<String>() {
        @Override
        public void onComplete() {
        }

        @Override
        public void onError(Throwable e) {
            Log.e("Throwable ERROR", e.getMessage());
        }

        @Override
        public void onSubscribe(Disposable d) {
            disposables.add(d);
        }

        @Override
        public void onNext(String value) {
            textViewWeather.append(value);
        }
    };

    /*Observable that emits weather as a string to subscribed Observers after making a background API call to fetch it every 3 seconds
    We need to map emitted interval Long values to string weather which we get from an API call inside map function*/
    Observable fetchWeatherInterval = Observable.interval(3, TimeUnit.SECONDS)
            .map(new Function<Long, String>() {
                @Override
                public String apply(Long aLong) throws Exception {
                    return getWeather("http://samples.openweathermap.org/data/2.5/weather?", "London,uk", "b1b15e88fa797225412429c1c50c122a1");
                }
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    /*Observer that listens above Observable for emitted items and displays them on MainThread
       We add this Observer to disposables to dispose onPause()
       We handle all errors from subscribed Observables inside Observer as well*/
    Observer displayWeatherInterval = new Observer<String>() {

        @Override
        public void onError(Throwable e) {
            Log.e("Throwable ERROR", e.getMessage());
        }

        @Override
        public void onComplete() {
        }

        @Override
        public void onSubscribe(Disposable d) {
            disposables.add(d);
        }

        @Override
        public void onNext(String value) {
            textViewWeatherInterval.append(value);
        }
    };

    //Observable to zip with bellow Observable that emits weather as a string after making a background API call to fetch it
    Observable<String> fetchWeatherOnce = Observable.fromCallable(new Callable<String>() {
        @Override
        public String call() throws Exception {
            return getWeather("http://samples.openweathermap.org/data/2.5/weather?", "London,uk", "b1b15e88fa797225412429c1c50c122a1");
        }
    })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    //Observable to zip with above Observable that emits weather as a string after making a background API call to fetch it
    Observable<String> fetchWeatherTwice = Observable.fromCallable(new Callable<String>() {
        @Override
        public String call() throws Exception {
            return getWeather("http://samples.openweathermap.org/data/2.5/weather?", "London,uk", "b1b15e88fa797225412429c1c50c122a1");
        }
    })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    //Observable that zip above Observables together and emits their combined values
    Observable<String> zipWeather = Observable.zip(fetchWeatherOnce, fetchWeatherTwice,
            new BiFunction<String, String, String>() {
                @Override
                public String apply(String londonWeather, String franceWeather) throws Exception {
                    return londonWeather + franceWeather;
                }
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    /*Observer that listens above Observable for emitted items and displays them on MainThread
   We add this Observer to disposables to dispose onPause()
   We handle all errors from subscribed Observables inside Observer as well, this includes both zipped Observables Exceptions*/
    Observer<String> displayWeatherDouble = new Observer<String>() {
        @Override
        public void onComplete() {}

        @Override
        public void onError(Throwable e) {
            Log.e("Throwable ERROR", e.getMessage());
        }

        @Override
        public void onSubscribe(Disposable d) {
            disposables.add(d);
        }

        @Override
        public void onNext(String value) {
            textViewWeatherDouble.append(value);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Get all views IDs
        initializeIds();
        //Load savedInstances if any
        if (savedInstanceState != null) {
            textViewWeather.setText(savedInstanceState.getString("weather"));
            textViewWeatherInterval.setText(savedInstanceState.getString("interval_weather"));
            textViewWeatherDouble.setText(savedInstanceState.getShort("double_weather"));
        }
        //On click subscribe first Observable and Observer to get weather
        buttonFetchWeather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchWeather.subscribe(displayWeather);
            }
        });
        //On click subscribe second Observable and Observer to get weather every 3 seconds
        buttonFetchIntervalWeather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchWeatherInterval.subscribe(displayWeatherInterval);
            }
        });
        //On click subscribe rest Observables and Observers (2 Observables zipped into a third that emits their combined values to a subscribed Observer)
        buttonFetchDoubleWeather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                zipWeather.subscribe(displayWeatherDouble);
            }
        });

    }
    // onPause() dispose subscriptions
    @Override
    public void onPause() {
        super.onPause();
        disposables.clear();
    }

    void initializeIds() {
        textViewWeather = (TextView) findViewById(R.id.text_view_weather);
        textViewWeatherInterval = (TextView) findViewById(R.id.text_view_weather_interval);
        textViewWeatherDouble = (TextView) findViewById(R.id.text_view_weather_double);
        buttonFetchWeather = (Button) findViewById(R.id.button_get_weather);
        buttonFetchIntervalWeather = (Button) findViewById(R.id.button_get_weather_interval);
        buttonFetchDoubleWeather = (Button) findViewById(R.id.button_get_weather_double);
    }
    //Method that returns weather, it uses okHTTP3 library to make the call for the weather JSON which we parse into a string
    public String getWeather(String url, String param_1, String param_2) throws IOException {
        //Create a URL with params
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        urlBuilder.addQueryParameter("q", param_1);
        urlBuilder.addQueryParameter("appid", param_2);
        //Build a Request from above url
        Request request = new Request.Builder()
                .url(urlBuilder.build().toString())
                .build();
        //Save Response from above Requests call (inside response.body().toString() we can find our JSON formatted weather to parse as string)
        Response response = client.newCall(request).execute();
        //Parse weather JSON
        String msg = "";
        try {
            JSONObject weatherJSON = new JSONObject(response.body().string());
            String city = weatherJSON.getString("name");
            double temperature = weatherJSON.getJSONObject("main").getDouble("temp");
            msg = city + ": " + String.valueOf(temperature) + " F \n";
        } catch (JSONException e) {
            Log.e("JSON ERROR", e.getMessage());
            msg = e.getMessage();
        }
        //Return weather as string
        return msg;
    }
    //Save textView values to load onCreate()
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("weather", textViewWeather.getText().toString());
        outState.putString("interval_weather", textViewWeatherInterval.getText().toString());
        outState.putString("double_weather", textViewWeatherDouble.getText().toString());
        super.onSaveInstanceState(outState);
    }
}
