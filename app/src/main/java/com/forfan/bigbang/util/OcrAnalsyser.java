package com.forfan.bigbang.util;

import android.text.TextUtils;

import com.forfan.bigbang.component.base.BaseActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.LanguageCodes;
import com.microsoft.projectoxford.vision.contract.Line;
import com.microsoft.projectoxford.vision.contract.OCR;
import com.microsoft.projectoxford.vision.contract.Region;
import com.microsoft.projectoxford.vision.contract.Word;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;
import com.microsoft.projectoxford.vision.rest.WebServiceRequest;

import java.io.IOException;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by wangyan-pd on 2016/11/19.
 */

public class OcrAnalsyser {
    //别人的 00b0e581e4124a2583ea7dba57aaf281
    // 我自己的 56c87e179c084cfaae9b70a2f58fa8d3
    //彭露的 9e88939475894dec85a2019fd36243be
    String[] keys = {"00b0e581e4124a2583ea7dba57aaf281", "9e88939475894dec85a2019fd36243be", "56c87e179c084cfaae9b70a2f58fa8d3"};
    int currentIndex = 0;
    private static OcrAnalsyser instance = new OcrAnalsyser();
    VisionServiceRestClient client = new VisionServiceRestClient(keys[currentIndex]);
    private String img_path;
    Observable.OnSubscribe<OCR> mOnSubscrube = new Observable.OnSubscribe<OCR>() {
        @Override
        public void call(Subscriber<? super OCR> subscriber) {
            byte[] data = IOUtil.getBytes(img_path);
            try {
                String ocr = client.recognizeText(data, LanguageCodes.AutoDetect, verticalOrentation);
                if (!TextUtils.isEmpty(ocr)) {
                    OCR ocrItem = new Gson().fromJson(ocr, new TypeToken<OCR>() {
                    }.getType());
                    subscriber.onNext(ocrItem);
                }
            } catch (VisionServiceException e) {
                e.printStackTrace();
                subscriber.onError(e);
            } catch (IOException e) {
                e.printStackTrace();
                subscriber.onError(e);
            }
        }
    };
    private boolean verticalOrentation = true;
    Observable.OnSubscribe<OCR> mOnSubscrube1 = new Observable.OnSubscribe<OCR>() {
        @Override
        public void call(Subscriber<? super OCR> subscriber) {

            try {
                String ocr = client.recognizeText(img, LanguageCodes.AutoDetect, verticalOrentation);
                client.setOnTimeUseUp(new WebServiceRequest.OnResult() {
                    @Override
                    public void onTimeUseUp() {
                        //返回403
                        currentIndex = (currentIndex + 1) % keys.length;
                        client = new VisionServiceRestClient(keys[currentIndex]);
                        subscriber.onError(new IOException("次数使用超出限额，为您切换服务器，请点击重试"));
                    }

                    @Override
                    public void onSuccess() {

                    }
                });
                if (!TextUtils.isEmpty(ocr)) {
                    OCR ocrItem = new Gson().fromJson(ocr, new TypeToken<OCR>() {
                    }.getType());
                    subscriber.onNext(ocrItem);
                }
            } catch (VisionServiceException e) {
                e.printStackTrace();
                subscriber.onError(e);
            } catch (IOException e) {
                e.printStackTrace();
                subscriber.onError(e);
            }
        }
    };
    private byte[] img;

    public static OcrAnalsyser getInstance() {
        return instance;
    }

    public interface CallBack {
        void onSucess(OCR ocr);

        void onFail(Throwable throwable);
    }

    public void analyse(BaseActivity activity, String img_path, boolean isVertical, CallBack callback) {
        if (callback == null)
            return;
        this.img_path = img_path;
        this.verticalOrentation = isVertical;
        Observable.create(mOnSubscrube)
                .subscribeOn(Schedulers.io())
                .compose(activity.bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> callback.onSucess(s),
                        throwable -> callback.onFail(throwable));
    }

    public void analyse(byte[] img, CallBack callback) {
        if (callback == null)
            return;
        this.img = img;
        try {
            Observable.create(mOnSubscrube1)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(s -> callback.onSucess(s),
                            throwable -> callback.onFail(throwable));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String getPasedMiscSoftText(OCR ocr) {

        String result = "";
        for (Region reg : ocr.regions) {
            for (Line line : reg.lines) {
                for (Word word : line.words) {
                    result += word.text + " ";
                }
                result += "\n";
            }
            result += "\n\n";
        }
        if (ocr.language.equalsIgnoreCase(LanguageCodes.ChineseSimplified) || ocr.language.equalsIgnoreCase(LanguageCodes.ChineseTraditional)) {
            result = result.replaceAll(" ", "");
        }
        if (TextUtils.isEmpty(result))
            result = "no text found";
        return result;
    }

}
