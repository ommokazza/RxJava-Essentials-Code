package com.packtpub.apps.rxjava_essentials.chapter8;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.packtpub.apps.rxjava_essentials.App;
import com.packtpub.apps.rxjava_essentials.R;
import com.packtpub.apps.rxjava_essentials.Utils;
import com.packtpub.apps.rxjava_essentials.chapter8.api.openweathermap.OpenWeatherMapApiManager;
import com.packtpub.apps.rxjava_essentials.chapter8.api.openweathermap.models.WeatherResponse;
import com.packtpub.apps.rxjava_essentials.chapter8.api.stackexchange.models.User;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.google.common.base.Preconditions.checkNotNull;

public class SoAdapter extends RecyclerView.Adapter<SoAdapter.ViewHolder> {

    private static ViewHolder.OpenProfileListener mProfileListener;

    private List<User> mUsers = new ArrayList<>();

    public SoAdapter(List<User> users) {
        mUsers = users;
    }

    public void updateUsers(List<User> users) {
        mUsers.clear();
        mUsers.addAll(users);
        notifyDataSetChanged();
    }

    @Override
    public SoAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.so_list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(SoAdapter.ViewHolder holder, int position) {
        if (position < mUsers.size()) {
            User user = mUsers.get(position);
            holder.setUser(user);
        }
    }

    @Override
    public int getItemCount() {
        return mUsers == null ? 0 : mUsers.size();
    }

    public void setOpenProfileListener(ViewHolder.OpenProfileListener listener) {
        mProfileListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final View mView;

        @InjectView(R.id.name)
        TextView name;

        @InjectView(R.id.city)
        TextView city;

        @InjectView(R.id.reputation)
        TextView reputation;

        @InjectView(R.id.user_image)
        ImageView user_image;

        @InjectView(R.id.city_image)
        ImageView city_image;


        public ViewHolder(View view) {
            super(view);
            ButterKnife.inject(this, view);
            mView = view;
        }

        private Observable<Bitmap> loadBitmap(String url) {
            return Observable
                    .create((ObservableEmitter<Bitmap> subscriber) -> {
                        ImageLoader.getInstance().displayImage(url, city_image, new ImageLoadingListener() {
                            @Override
                            public void onLoadingStarted(String imageUri, View view) {

                            }

                            @Override
                            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                                subscriber.onError(failReason.getCause());
                            }

                            @Override
                            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                                subscriber.onNext(loadedImage);
                                subscriber.onComplete();
                            }

                            @Override
                            public void onLoadingCancelled(String imageUri, View view) {
                                subscriber.onError(new Throwable("Image loading cancelled"));
                            }
                        });
                    });
        }

        public void setUser(User user) {
            name.setText(user.getDisplayName());
            city.setText(user.getLocation());
            reputation.setText(String.valueOf(user.getReputation()));

            ImageLoader.getInstance().displayImage(user.getProfileImage(), user_image);

            // We can't get weather infomation from open weather map.
            // Because from Oct. 2015, it request a valid APPID.
            //displayWeatherInfos(user);

            // ViewObservable does not exist in both rxandroid 1.x and rxandroid 2.x
            //ViewObservable.clicks(mView)
            //...

            mView.setOnClickListener(view -> {
                    checkNotNull(mProfileListener, "Must implement OpenProfileListener");

                    String url = user.getWebsiteUrl();
                    if (url != null && !url.equals("") && !url.contains("search")) {
                        mProfileListener.open(url);
                    } else {
                        mProfileListener.open(user.getLink());
                    }
            });
        }

        private void displayWeatherInfos(User user) {
            String location = user.getLocation();
            int separatorPosition = getSeparatorPosition(location);

            if (isCityValid(location)) {
                String city = getCity(location, separatorPosition);
                OpenWeatherMapApiManager.getInstance()
                        .getForecastByCity(city)
                        .filter(response -> response != null)
                        .filter(response -> response.getWeather().size() > 0)
                        .flatMap(response -> {
                            String url = getWeatherIconUrl(response);
                            return loadBitmap(url);
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<Bitmap>() {
                            @Override
                            public void onSubscribe(Disposable d) {
                            }

                            @Override
                            public void onComplete() {
                            }

                            @Override
                            public void onError(Throwable e) {
                                App.L.error(e.toString());
                            }

                            @Override
                            public void onNext(Bitmap icon) {
                                city_image.setImageBitmap(icon);
                            }
                        });
            }
        }

        private String getWeatherIconUrl(WeatherResponse weatherResponse) {
            return "http://openweathermap.org/img/w/" + weatherResponse.getWeather().get(0).getIcon() + ".png";
        }

        private boolean isCityValid(String location) {
            int separatorPosition = getSeparatorPosition(location);
            return !"".equals(location) && separatorPosition > -1;
        }

        private int getSeparatorPosition(String location) {
            int separatorPosition = -1;
            checkNotNull(location, "Location can't be null");
            separatorPosition = location.indexOf(",");
            return separatorPosition;
        }

        private String getCity(String location, int position) {
            if (location != null) {
                return location.substring(0, position);
            } else {
                return "";
            }
        }

        public interface OpenProfileListener {

            public void open(String url);
        }
    }
}
