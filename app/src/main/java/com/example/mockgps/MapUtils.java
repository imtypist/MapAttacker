package com.example.mockgps;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.Gravity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class MapUtils {
    public static void startGuide(Context context, String packageName, String fromLat, String fromLng, String toLat, String toLng) {
        if(!isAvailable(context, packageName) && !packageName.equals("com.google.android.apps.maps")){
            DisplayToast(context, "未安装该应用");
            return;
        }
        switch (packageName) {
            case "com.google.android.apps.maps":
                startNaviGoogle(context, fromLat, fromLng, toLat, toLng);
                break;
            case "com.autonavi.minimap":
                startNaviAmap(context, fromLat, fromLng, toLat, toLng);
                break;
            case "com.baidu.BaiduMap":
                startNaviBaidu(context, fromLat, fromLng, toLat, toLng);
                break;
            case "com.tecent.map":
                startNaviTencent(context, fromLat, fromLng, toLat, toLng);
                break;
            default:
                DisplayToast(context, "不支持该应用");
                break;
        }

    }

    private static void DisplayToast(Context context, String str) {
        Toast toast = Toast.makeText(context, str, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 220);
        toast.show();
    }

    //谷歌地图
    private static void startNaviGoogle(Context context, String fromLat, String fromLng, String toLat, String toLng) {
        try{
            Uri gmmIntentUri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin="+fromLat+","+fromLng+"&destination="+toLat+","+toLng+"&travelmode=driving&dir_action=navigate");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            context.startActivity(mapIntent);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    //高德地图
    // 终点是LatLng ll = new LatLng("你的纬度latitude","你的经度longitude");
    private static void startNaviAmap(Context context, String fromLat, String fromLng, String toLat, String toLng) {
        try {
            Intent intent = new Intent();
            intent.setData(Uri.parse("amapuri://route/plan/?slat="+fromLat+"&slon="+fromLng+"&dlat="+toLat+"&dlon="+toLng+"&dev=1&t=0"));
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //百度地图
    private static void startNaviBaidu(Context context, String fromLat, String fromLng, String toLat, String toLng) {
        try {
            Intent intent = new Intent();
            intent.setData(Uri.parse("baidumap://map/direction?region=beijing&origin="+fromLat+","+fromLng+"&destination="+toLat+","+toLng+"&coord_type=wgs84&mode=driving&src=andr.baidu.openAPIdemo"));
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //腾讯地图
    private static void startNaviTencent(Context context, String fromLat, String fromLng, String toLat, String toLng){
        try {
            //referer是官方示例，需要改成自己的
            Intent intent = new Intent();
            intent.setData(Uri.parse("qqmap://map/routeplan?type=drive&fromcoord="+fromLat+","+fromLng+"&tocoord="+toLat+","+toLng+"&referer=OB4BZ-D4W3U-B7VVO-4PJWW-6TKDJ-WPB77"));
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //验证各种导航地图是否安装
    private static boolean isAvailable(Context context, String packageName) {
        //获取packagemanager
        final PackageManager packageManager = context.getPackageManager();
        //获取所有已安装程序的包信息
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);
        //用于存储所有已安装程序的包名
        List<String> packageNames = new ArrayList<String>();
        //从pinfo中将包名字逐一取出，压入pName list中
        if (packageInfos != null) {
            for (int i = 0; i < packageInfos.size(); i++) {
                String packName = packageInfos.get(i).packageName;
                packageNames.add(packName);
            }
        }
        //判断packageNames中是否有目标程序的包名，有TRUE，没有FALSE
        return packageNames.contains(packageName);
    }


}