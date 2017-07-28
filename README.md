# Usage

Step1:
In styles.xml file define your basic AppTheme style, for instance the following
```xml
<!-- Base application theme. -->
<style name="AppTheme" parent="Theme.AppCompat.Light.DarkActionBar">
    <!-- Customize your theme here. -->
    <item name="colorPrimary">@color/colorPrimary</item>
    <item name="colorPrimaryDark">@color/colorPrimaryDark</item>
    <item name="colorAccent">@color/colorAccent</item>
</style>

<style name="AppTheme.NoActionBar">
    <item name="windowActionBar">false</item>
    <item name="windowNoTitle">true</item>
</style>
```
Step2: 
In AndroidManifest.xml file add permission and activity
```xml
<!--for permission-->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>

<application
    android:name=".app.ShareBaApp"
    android:allowBackup="true"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:theme="@style/AppTheme">
    
    <!--for activity-->
    <activity android:name=".medialist.ui.MediaListActivity" />
        
</application>        
```

Step3:
Create one Intent to use the medialist
```java
Intent intent = new Intent(ExampleMediaListActivity.this, MediaListActivity.class);
startActivityForResult(intent, MediaListActivity.MEDIA_LIST_REQUEST);
```

Step4:
Receive selected media data
```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == AppCompatActivity.RESULT_OK) {
        if (data != null && data.hasExtra(MediaListActivity.MEDIA_LIST_DATA)) {
            // get the data
            List<String> mediaPath = data.getStringArrayListExtra(MediaListActivity.MEDIA_LIST_DATA);
            for(String path: mediaPath) {
                Log.d(TAG, "path is : " + path);
            }
        }
    }
    super.onActivityResult(requestCode, resultCode, data);
}
```
