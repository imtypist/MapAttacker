import os
import threading
import time
import requests
import json
import math

'''navigation apps package name'''
GAODEMAP = 1
BAIDUMAP = 2
TENCENTMAP = 3
GOOGLEMAP = 4

'''
e.g., [(lat,lng),]

location_points = [[31.2050610000, 121.3266350000], [31.2050470000, 121.3264740000], [31.2050610000, 121.3262650000],
                   [31.2050520000, 121.3261310000], [31.2050430000, 121.3259640000], [31.2050290000, 121.3258680000],
                   [31.2050250000, 121.3257390000], [31.2050200000, 121.3256260000], [31.2050150000, 121.3254760000],
                   [31.2049970000, 121.3253210000], [31.2050060000, 121.3252130000], [31.2049920000, 121.3250630000],
                   [31.2049970000, 121.3249560000], [31.2049920000, 121.3248220000], [31.2049690000, 121.3247040000]]
'''

location_points = []


def update_location_gaode(device):
    cmd_prefix = "nox_adb.exe -s " + device + " shell "
    '''start navigation acitivity'''
    os.popen(cmd_prefix + "am start -d \"amapuri://route/plan/?slat=" + str(location_points[0][0]) + "'&'slon=" + str(
        location_points[0][1]) + "'&'dlat=" + str(location_points[-1][0]) + "'&'dlon=" + str(
        location_points[-1][1]) + "'&'dev=1'&'t=0\"")
    time.sleep(30)
    '''start navigation'''
    os.popen(cmd_prefix + "input tap 850 1560")
    for i in range(len(location_points)):
        if i == (len(location_points)-1):
            d = 1
            delta_lat = 0
            delta_lng = 0
        else:
            d = int(distance(location_points[i], location_points[i+1]))
            delta_lat = (location_points[i+1][0] - location_points[i][0]) / d
            delta_lng = (location_points[i+1][1] - location_points[i][1]) / d
        for j in range(d):
            os.popen(cmd_prefix + "setprop persist.nox.gps.latitude " + str(location_points[i][0] + j*delta_lat))
            os.popen(cmd_prefix + "setprop persist.nox.gps.longitude " + str(location_points[i][1] + j*delta_lng))
            time.sleep(2)


def update_location_baidu(device):
    cmd_prefix = "nox_adb.exe -s " + device + " shell "
    '''start navigation acitivity'''
    os.popen(cmd_prefix + "am start -d \"baidumap://map/direction?origin=" + str(location_points[0][0]) + "," + str(
        location_points[0][1]) + "'&'destination=" + str(location_points[-1][0]) + "," + str(
        location_points[-1][1]) + "'&'coord_type=wgs84'&'mode=driving'&'src=com.baidu.BaiduMap\"")
    time.sleep(30)
    '''start navigation'''
    os.popen(cmd_prefix + "input tap 840 1520")
    for i in range(len(location_points)):
        if i == (len(location_points)-1):
            d = 1
            delta_lat = 0
            delta_lng = 0
        else:
            d = int(distance(location_points[i], location_points[i+1]))
            delta_lat = (location_points[i+1][0] - location_points[i][0]) / d
            delta_lng = (location_points[i+1][1] - location_points[i][1]) / d
        for j in range(d):
            os.popen(cmd_prefix + "setprop persist.nox.gps.latitude " + str(location_points[i][0] + j*delta_lat))
            os.popen(cmd_prefix + "setprop persist.nox.gps.longitude " + str(location_points[i][1] + j*delta_lng))
            time.sleep(2)


def update_location_tencent(device):
    cmd_prefix = "nox_adb.exe -s " + device + " shell "
    os.popen(cmd_prefix + "setprop persist.nox.gps.latitude " + str(location_points[0][0]))
    os.popen(cmd_prefix + "setprop persist.nox.gps.longitude " + str(location_points[0][1]))
    '''start navigation acitivity'''
    from_lng, from_lat = wgs84_to_gcj02(location_points[0][1], location_points[0][0])
    to_lng, to_lat = wgs84_to_gcj02(location_points[-1][1], location_points[-1][0])
    os.popen(cmd_prefix + "am start -d \"qqmap://map/routeplan?type=drive'&'fromcoord=" + str(
        from_lat) + "," + str(from_lng) + "'&'tocoord=" + str(to_lat) + "," + str(to_lng)
             + "'&'referer=OB4BZ-D4W3U-B7VVO-4PJWW-6TKDJ-WPB77\"")
    time.sleep(30)
    '''start navigation'''
    os.popen(cmd_prefix + "input tap 840 1570")
    for i in range(len(location_points)):
        if i == (len(location_points)-1):
            d = 1
            delta_lat = 0
            delta_lng = 0
        else:
            d = int(distance(location_points[i], location_points[i+1]))
            delta_lat = (location_points[i+1][0] - location_points[i][0]) / d
            delta_lng = (location_points[i+1][1] - location_points[i][1]) / d
        for j in range(d):
            os.popen(cmd_prefix + "setprop persist.nox.gps.latitude " + str(location_points[i][0] + j*delta_lat))
            os.popen(cmd_prefix + "setprop persist.nox.gps.longitude " + str(location_points[i][1] + j*delta_lng))
            time.sleep(2)


def update_location_google(device):
    cmd_prefix = "nox_adb.exe -s " + device + " shell "
    os.popen(cmd_prefix + "am start -d \"https://www.google.com/maps/dir/?api=1'&'origin=" + str(
        location_points[0][0]) + "," + str(location_points[0][1]) + "'&'destination=" + str(
        location_points[-1][0]) + "," + str(location_points[-1][1]) + "'&'travelmode=driving'&'dir_action=navigate\"")
    for point in location_points:
        os.popen(cmd_prefix + "setprop persist.nox.gps.latitude " + str(point[0]))
        os.popen(cmd_prefix + "setprop persist.nox.gps.longitude " + str(point[1]))
        time.sleep(1)


def request_location_points():
    location_points.clear()
    url = "http://hhmoumoumouhh.51vip.biz/web_war_exploded2/LoginServlet"
    res = requests.post(url, data={"AccountNumber": "Tommy", "Password": "2345678"})
    ret_data = json.loads(res.text)
    if ret_data['params']['Result'] == "success":
        for obj in ret_data['points']:
            location_points.append([float(obj['latitude']), float(obj['longitude'])])
        print(location_points)
    else:
        return -1


x_pi = 3.14159265358979324 * 3000.0 / 180.0
pi = 3.1415926535897932384626  # π
a = 6378245.0  # 长半轴
ee = 0.00669342162296594323  # 偏心率平方


def wgs84_to_bd09(lon, lat):
    lon, lat = wgs84_to_gcj02(lon, lat)
    return gcj02_to_bd09(lon, lat)


def bd09_to_wgs84(bd_lon, bd_lat):
    lon, lat = bd09_to_gcj02(bd_lon, bd_lat)
    return gcj02_to_wgs84(lon, lat)


def gcj02_to_bd09(lng, lat):
    """
	火星坐标系(GCJ-02)转百度坐标系(BD-09)
	谷歌、高德——>百度
	:param lng:火星坐标经度
	:param lat:火星坐标纬度
	:return:
	"""
    z = math.sqrt(lng * lng + lat * lat) + 0.00002 * math.sin(lat * x_pi)
    theta = math.atan2(lat, lng) + 0.000003 * math.cos(lng * x_pi)
    bd_lng = z * math.cos(theta) + 0.0065
    bd_lat = z * math.sin(theta) + 0.006
    return [bd_lng, bd_lat]


def bd09_to_gcj02(bd_lon, bd_lat):
    """
	百度坐标系(BD-09)转火星坐标系(GCJ-02)
	百度——>谷歌、高德
	:param bd_lat:百度坐标纬度
	:param bd_lon:百度坐标经度
	:return:转换后的坐标列表形式
	"""
    x = bd_lon - 0.0065
    y = bd_lat - 0.006
    z = math.sqrt(x * x + y * y) - 0.00002 * math.sin(y * x_pi)
    theta = math.atan2(y, x) - 0.000003 * math.cos(x * x_pi)
    gg_lng = z * math.cos(theta)
    gg_lat = z * math.sin(theta)
    return [gg_lng, gg_lat]


def wgs84_to_gcj02(lng, lat):
    """
	WGS84转GCJ02(火星坐标系)
	:param lng:WGS84坐标系的经度
	:param lat:WGS84坐标系的纬度
	:return:
	"""
    if out_of_china(lng, lat):  # 判断是否在国内
        return [lng, lat]
    dlat = _transformlat(lng - 105.0, lat - 35.0)
    dlng = _transformlng(lng - 105.0, lat - 35.0)
    radlat = lat / 180.0 * pi
    magic = math.sin(radlat)
    magic = 1 - ee * magic * magic
    sqrtmagic = math.sqrt(magic)
    dlat = (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtmagic) * pi)
    dlng = (dlng * 180.0) / (a / sqrtmagic * math.cos(radlat) * pi)
    mglat = lat + dlat
    mglng = lng + dlng
    return [mglng, mglat]


def gcj02_to_wgs84(lng, lat):
    """
	GCJ02(火星坐标系)转GPS84
	:param lng:火星坐标系的经度
	:param lat:火星坐标系纬度
	:return:
	"""
    if out_of_china(lng, lat):
        return [lng, lat]
    dlat = _transformlat(lng - 105.0, lat - 35.0)
    dlng = _transformlng(lng - 105.0, lat - 35.0)
    radlat = lat / 180.0 * pi
    magic = math.sin(radlat)
    magic = 1 - ee * magic * magic
    sqrtmagic = math.sqrt(magic)
    dlat = (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtmagic) * pi)
    dlng = (dlng * 180.0) / (a / sqrtmagic * math.cos(radlat) * pi)
    mglat = lat + dlat
    mglng = lng + dlng
    return [lng * 2 - mglng, lat * 2 - mglat]


def _transformlat(lng, lat):
    ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + \
          0.1 * lng * lat + 0.2 * math.sqrt(math.fabs(lng))
    ret += (20.0 * math.sin(6.0 * lng * pi) + 20.0 *
            math.sin(2.0 * lng * pi)) * 2.0 / 3.0
    ret += (20.0 * math.sin(lat * pi) + 40.0 *
            math.sin(lat / 3.0 * pi)) * 2.0 / 3.0
    ret += (160.0 * math.sin(lat / 12.0 * pi) + 320 *
            math.sin(lat * pi / 30.0)) * 2.0 / 3.0
    return ret


def _transformlng(lng, lat):
    ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + \
          0.1 * lng * lat + 0.1 * math.sqrt(math.fabs(lng))
    ret += (20.0 * math.sin(6.0 * lng * pi) + 20.0 *
            math.sin(2.0 * lng * pi)) * 2.0 / 3.0
    ret += (20.0 * math.sin(lng * pi) + 40.0 *
            math.sin(lng / 3.0 * pi)) * 2.0 / 3.0
    ret += (150.0 * math.sin(lng / 12.0 * pi) + 300.0 *
            math.sin(lng / 30.0 * pi)) * 2.0 / 3.0
    return ret


def out_of_china(lng, lat):
    """
	判断是否在国内，不在国内不做偏移
	:param lng:
	:param lat:
	:return:
	"""
    return not (73.66 < lng < 135.05 and 3.86 < lat < 53.55)


def distance(origin, destination):
    lat1, lon1 = origin
    lat2, lon2 = destination
    radius = 6371 # km

    dlat = math.radians(lat2-lat1)
    dlon = math.radians(lon2-lon1)
    a = math.sin(dlat/2) * math.sin(dlat/2) + math.cos(math.radians(lat1)) \
        * math.cos(math.radians(lat2)) * math.sin(dlon/2) * math.sin(dlon/2)
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))
    d = radius * c
    '''单位为m'''
    return d*1000


if __name__ == '__main__':
    ret = request_location_points()
    if ret == -1:
    	print("[Error] fail to request data.")
    	exit(ret)

    for point in location_points:
        point[1], point[0] = gcj02_to_wgs84(point[1], point[0])
    os.popen("Nox.exe")
    ret = os.popen("nox_adb.exe devices")
    '''
	e.g.,
	List of devices attached
	127.0.0.1:62001 device
	
	'''
    simulators = ret.readlines()[1:]
    print(simulators)

    while True:
        option = input("Do you want to attack on 1) GaodeMap 2) BaiduMap 3)TencentMap 4)GoogleMap [1-4]? : ")
        if option == 'n':
            exit(0)
        '''check the legality of option'''
        target_function_name = None
        option = int(option)
        if option == GAODEMAP:
            target_function_name = update_location_gaode
        elif option == BAIDUMAP:
            target_function_name = update_location_baidu
        elif option == TENCENTMAP:
            target_function_name = update_location_tencent
        elif option == GOOGLEMAP:
            target_function_name = update_location_google
            print("[Info] Google map has not been supported yet.")
            continue
        else:
            print("[Error] Your input '" + str(option) + "' is an illegal number. Input 'n' to exit.")
            continue
        '''start simulation'''
        threads = []
        for simulator in simulators:
            if simulator == '\n':
                continue
            device = simulator.split('\t')[0]
            t = threading.Thread(target=target_function_name, args=(device,))
            threads.append(t)
            t.start()

        for t in threads:
            t.join()  
