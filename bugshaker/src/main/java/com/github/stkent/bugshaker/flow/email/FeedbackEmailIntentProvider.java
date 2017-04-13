/*
 * Copyright 2016 Stuart Kent
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.github.stkent.bugshaker.flow.email;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.security.auth.login.LoginException;

public final class FeedbackEmailIntentProvider {

    private static final String DEFAULT_EMAIL_SUBJECT_LINE_SUFFIX = " Android App Feedback";

    @NonNull
    private final GenericEmailIntentProvider genericEmailIntentProvider;

    @NonNull
    private final App app;

    @NonNull
    private final Environment environment;

    @NonNull
    private final Device device;

    public FeedbackEmailIntentProvider(
            @NonNull final Context context,
            @NonNull final GenericEmailIntentProvider genericEmailIntentProvider) {

        this.genericEmailIntentProvider = genericEmailIntentProvider;
        this.app = new App(context);
        this.environment = new Environment();
        this.device = new Device(context);
    }

    @NonNull
    /* default */ Intent getFeedbackEmailIntent(
            @NonNull final String[] emailAddresses,
            @Nullable final String userProvidedEmailSubjectLine, final String logFilePath) {

        final String emailSubjectLine = getEmailSubjectLine(userProvidedEmailSubjectLine);
        final String emailBody = getApplicationInfoString(app, environment, device, logFilePath);

        return genericEmailIntentProvider
                .getEmailIntent(emailAddresses, emailSubjectLine, emailBody);
    }

    @NonNull
    /* default */ Intent getFeedbackEmailIntent(
            @NonNull final String[] emailAddresses,
            @Nullable final String userProvidedEmailSubjectLine,
            final String logFilePath,
            @NonNull final Uri screenshotUri
            ) {

        final String emailSubjectLine = getEmailSubjectLine(userProvidedEmailSubjectLine);
        final String emailBody = getApplicationInfoString(app, environment, device, logFilePath);

        return genericEmailIntentProvider
                .getEmailWithAttachmentIntent(
                        emailAddresses, emailSubjectLine, emailBody, screenshotUri);
    }

    @NonNull
    private String getApplicationInfoString(
            @NonNull final App app,
            @NonNull final Environment environment,
            @NonNull final Device device,
            final String logFilePath) {

        final String androidVersionString = String.format(
                "%s (%s)", environment.getAndroidVersionName(), environment.getAndroidVersionCode());

        final String appVersionString = String.format("%s (%s)", app.getVersionName(), app.getVersionCode());


        final String logString =readLogs(logFilePath);
        // @formatter:off
        return "Time Stamp: " + getCurrentUtcTimeStringForDate(new Date()) + "\n"
                + "App Version: " + appVersionString + "\n"
                + "Install Source: " + app.getInstallSource() + "\n"
                + "Android Version: " + androidVersionString + "\n"
                + "Device Manufacturer: " + device.getManufacturer() + "\n"
                + "Device Model: " + device.getModel() + "\n"
                + "Display Resolution: " + device.getResolution() + "\n"
                + "Display Density (Actual): " + device.getActualDensity() + "\n"
                + "Display Density (Bucket) " + device.getDensityBucket() + "\n"
                + "---------------------\n\n"
                + logString
                + "---------------------\n\n";

    }


    private String readLogs(String logFilePath) {
        final String path = logFilePath;
        String content = ""; //文件内容字符串
        //打开文件
        File file = new File(path);

        File lastModifyFile = null;

        //如果path是传递过来的参数，可以做一个非目录的判断
        if (file.isDirectory()) {
            Log.d("TestFile", "The File doesn't not exist.");

            //获取最新的日志文件
            File[] logFiles = file.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (pathname.isDirectory())
                        return false;
                    else
                        return true;

                }
            });
            if (logFiles.length > 2) {
                List<File> fileList = Arrays.asList(logFiles);//将需要的子文件信息存入到FileInfo里面
                Collections.sort(fileList, new Comparator<File>() {
                    @Override
                    public int compare(File file1, File file2) {
                        if (file1.lastModified() < file2.lastModified()) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                });//通过重写Comparator的实现类FileComparator来实现按文件创建时间排序。

                lastModifyFile = fileList.get(0);
            } else if (logFiles.length == 1) {
                lastModifyFile = logFiles[0];
            }
        } else {
            lastModifyFile = file;
        }

        try {
            InputStream instream = new FileInputStream(lastModifyFile);
            if (instream != null) {
                InputStreamReader inputreader = new InputStreamReader(instream);
                BufferedReader buffreader = new BufferedReader(inputreader);
                String line;
                //分行读取
                while ((line = buffreader.readLine()) != null) {
                    content += line + "\n";
                }
                instream.close();
            }
        } catch (java.io.FileNotFoundException e) {
            Log.d("TestFile", "The File doesn't not exist.");
        } catch (IOException e) {
            Log.d("TestFile", e.getMessage());
        }

        return content;
    }

    @NonNull
    private String getEmailSubjectLine(@Nullable final String userProvidedEmailSubjectLine) {
        if (userProvidedEmailSubjectLine != null) {
            return userProvidedEmailSubjectLine;
        }

        return app.getName() + DEFAULT_EMAIL_SUBJECT_LINE_SUFFIX;
    }

    @NonNull
    private String getCurrentUtcTimeStringForDate(final Date date) {
        final SimpleDateFormat simpleDateFormat
                = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss z", Locale.getDefault());

        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        return simpleDateFormat.format(date);
    }

}
