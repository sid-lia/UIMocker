package com.llx278.uimocker2;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * 一些常用的api结合，组合了{@link Clicker},{@link Searcher},{@link ViewGetter},{@link Waiter}等一些类，
 * 使得使用更加的方便一些。
 * @author llx
 */
public class Solo {

    private static final String TAG = "Solo";
    private static Solo INSTANCE;

    private final Clicker mClicker;
    private final Scroller mScroller;
    private final Searcher mSearcher;
    private final Sender mSender;
    private final Sleeper mSleeper;
    private final ViewGetter mViewGetter;
    private final Waiter mWaiter;
    private final ActivityUtils mActivityUtils;
    private final MyInstrumentation mInstrumentation;
    private final DialogUtils mDialogUtils;
    private final Config mConfig;
    private final Parser mParser;
    private final Gesture mGesture;

    private Solo(Activity activity, Context context, Config config) {
        mConfig = (config == null ? new Config() : config);
        mInstrumentation = MyInstrumentation.getInstance(context);
        mSleeper = new Sleeper(mConfig.sleepDuration, mConfig.defaultSleepMiniDuration);
        mActivityUtils = new ActivityUtils(mInstrumentation,activity);
        mViewGetter = new ViewGetter(mInstrumentation, mSleeper);
        mScroller = new Scroller(mInstrumentation, mViewGetter, mSleeper);
        mSearcher = new Searcher(mViewGetter, mScroller, mSleeper, mConfig);
        mWaiter = new Waiter(mInstrumentation, mActivityUtils, mViewGetter, mSearcher, mConfig,mSleeper);
        mSender = new Sender(mInstrumentation, mSleeper);
        mDialogUtils = new DialogUtils(mInstrumentation, mActivityUtils, mViewGetter, mSleeper);
        mClicker = new Clicker(mActivityUtils, mViewGetter, mSender, mInstrumentation, mSleeper,
                mSearcher, mDialogUtils);
        mParser = new Parser(mScroller,mInstrumentation,mSleeper);
        mGesture = new Gesture(mInstrumentation,mActivityUtils,mSleeper);
    }

    public static Solo getInstance(Activity activity,Context context,Config config) {
        if (INSTANCE == null) {
            INSTANCE = new Solo(activity,context,config);
        }
        return INSTANCE;
    }

    /**
     * 返回当前activity的所有可见的View
     *
     * @return 获得到的view
     */
    public ArrayList<View> getViewList() {
        return mViewGetter.getViews();
    }

    /**
     * 返回当前activity的所有可见的View
     *
     * @param delay 延时delay ms以后开始获取
     * @return 获得到的view
     */
    public ArrayList<View> getViewList(long delay) {
        return getViewList(null, true, 0);
    }

    /**
     * 返回parent的所有可见的子view
     *
     * @param parent 指定的parentView
     * @return 返回子View的列表
     */
    public ArrayList<View> getViewList(View parent) {
        return getViewList(parent, true, 0);
    }

    /**
     * 返回parent的所有可见的子view
     *
     * @param parent 父view
     * @param delay  延时delay ms以后获取
     * @return 返回子view的列表
     */
    public ArrayList<View> getViewList(View parent, long delay) {
        return getViewList(parent, true, delay);
    }

    /**
     * 获得parent所有的子view如果parent为null，则返回当前activity里面的
     *
     * @param parent                待获取的view
     * @param onlySufficientVisible true 只返回可见的，false 返回所有
     * @param delay                 延时delay ms以后获取
     * @return 返回子view的列表
     */
    public ArrayList<View> getViewList(View parent, boolean onlySufficientVisible, long delay) {
        if (delay > 0) {
            mSleeper.sleep(delay);
        }
        return mViewGetter.getViews(parent, onlySufficientVisible);
    }


    /**
     * 返回view的id等于给定id的所有view
     *
     * @param id 指定的id
     * @return 符合条件的view的列表
     */
    public ArrayList<View> getViewList(int id) {
        return getViewList(id, null, true, 0);
    }

    /**
     * 返回view的id等于给定id的所有view
     *
     * @param id     指定的id
     * @param parent 待查找的父view，如果parent为null，则查找当前activity内的所有view
     * @return 符合条件的view的列表
     */
    public ArrayList<View> getViewList(int id, View parent) {
        return getViewList(id, parent, true, 0);
    }

    /**
     * 返回view的id等于给定id的所有view
     *
     * @param id                    指定的id
     * @param parent                待查找的父view，如果parent为null，则查找当前activity里的所有view
     * @param onlySufficientVisible true 只返回可见的，false 则返回所有
     * @param delay                 延时delay ms后开始查找
     * @return
     */
    public ArrayList<View> getViewList(int id, View parent, boolean onlySufficientVisible, long delay) {
        if (delay > 0) {
            mSleeper.sleep(delay);
        }
        return mViewGetter.getViewsById(id, parent, onlySufficientVisible);
    }

    /**
     * 根据id找到当前activity里面的view
     *
     * @param id 指定的id
     * @return 找的view，如果为空，则说明在默认给定的时间里面没有找到
     */
    public View findViewById(int id) {
        return findViewById(id, mConfig.defaultWaitTimeout);
    }

    /**
     * 根据id找到当前activity里面的view
     *
     * @param id      指定的id
     * @param timeout 超时时间
     * @return 找的view，如果为空，则说明在给定的时间里面没有找到
     */
    public View findViewById(int id, long timeout) {
        long endTime = SystemClock.uptimeMillis() + timeout;
        while (SystemClock.uptimeMillis() < endTime) {
            mSleeper.sleep();
            Activity currentActivity = mActivityUtils.getCurrentActivity();
            if (currentActivity == null) {
                Logger.d(TAG,"Solo.findViewById(int) currentActivity is null");
                continue;
            }
            View view = currentActivity.findViewById(id);
            if (view != null) {
                return view;
            }
        }
        return null;
    }

    /**
     * 根据id找到给定parent里面的view
     *
     * @param id     指定的id
     * @param parent 待寻找的view
     * @return 找到的view，如果为空则说明在默认超时时间里面没有找到
     */
    public View findViewById(int id,View parent) {
        return findViewById(id,parent,mConfig.defaultWaitTimeout);
    }

    /**
     * 根据id找到给定parent里面的view
     *
     * @param id     指定的id
     * @param parent 待寻找的view
     * @param timeout 超时时间
     * @return 找到的view，如果为空则说明在超时时间里面没有找到
     */
    public View findViewById(int id, View parent, long timeout) {
        if (parent == null) {
            return null;
        }

        long endTime = SystemClock.uptimeMillis() + timeout;
        while (SystemClock.uptimeMillis() < endTime) {
            mSleeper.sleep();
            View v = parent.findViewById(id);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    /**
     * 返回所有符合class的View，默认T的子类也会返回
     *
     * @param viewClass 待查询的viewlass
     * @return 符合条件的view的列表
     */
    public <T extends View> ArrayList<T> getViewList(Class<T> viewClass,boolean immediately) {
        return mViewGetter.getViewsByClass(viewClass);
    }

    /**
     * 根据指定的类名，返回所有符合的view
     *
     * @param className 类名
     * @param parent    如果parent为null，则从当前的activity里面开始寻找
     * @return 符合指定类名字的view
     */
    public ArrayList<View> getCustomViews(String className, View parent) {
        return mViewGetter.getViewsByName(className, parent, true);
    }

    /**
     * 根据指定的类名，返回第一个符合条件的view
     *
     * @param className 类名
     * @param parent    如果parent为null，则从当前的activity里面开始寻找
     * @return 符合指定类名字的view
     */
    public View getCustomView(String className, View parent) {
        ArrayList<View> viewsToReturn = getCustomViews(className, parent);
        if (viewsToReturn != null && !viewsToReturn.isEmpty()) {
            return viewsToReturn.get(0);
        }
        return null;
    }

    /**
     * 返回由classname指定的，并由filter过滤后的view
     *
     * @param className 类名
     * @param parent    如果parent为null，则从当前的activity里面开始寻找
     * @param filter    自定义过滤的方法
     * @return 返回符合条件的view
     */
    public ArrayList<View> getCustomViewsByFilter(String className, View parent, Filter filter) {
        return mSearcher.searchForNamesByFilter(className, parent, filter, mConfig.defaultSearchTimeout, true, true);
    }

    /**
     * 返回由classname指定的，并由filter过滤后的view，注意，这里只返回第一次匹配成功的view，
     * 即{@link Filter#match(View)}第一次返回true的那个view
     *
     * @param className 类名
     * @param parent    如果parent为null，则从当前的activity里面开始寻找
     * @param filter    自定义过滤的方法
     * @return 返回符合条件的view
     */
    public View getCustomViewByFilter(String className, View parent, Filter filter) {
        return mSearcher.searchForNameByFilter(className, parent, filter, mConfig.defaultSearchTimeout, true, true);
    }

    public ArrayList<TextView> getTextList(String regex) {
        return getTextList(regex, mConfig.defaultSearchTimeout);
    }

    public ArrayList<TextView> getTextList(String regex, long timeout) {
        return mSearcher.searchForTextList(regex, timeout, true, true);
    }

    public TextView getText(String regex) {
        return getText(regex, mConfig.defaultSearchTimeout);
    }

    public TextView getText(String regex, long timeout) {
        return mWaiter.waitForTextAndGet(regex, timeout);
    }

    public ArrayList<EditText> getEditTextList(String regex) {
        return getEditTextList(regex, mConfig.defaultSearchTimeout);
    }

    /**
     * 返回带有指定hint的所有editText，如果regex为null，则当前可见的
     * 所有editText
     *
     * @param regex   符合正则表达式的hint
     * @param timeout 超时时间
     * @return 符合条件的editText列表
     */
    public ArrayList<EditText> getEditTextList(String regex, long timeout) {
        return mSearcher.searchForEditTextList(regex, timeout, true, true);
    }

    public EditText getEditText(String regex) {
        return getEditText(regex, mConfig.defaultSearchTimeout);
    }

    public EditText getEditText(String regex, long timeout) {
        return mSearcher.searchForEditText(regex, timeout, true, true);
    }


    /**
     * 获得指定view的最顶端的view
     *
     * @param view 待寻找的view
     * @return 最顶端的view
     */
    public View getTopParent(View view) {
        return mViewGetter.getTopParent(view);
    }

    public View[] getWindowDecorViews() {
        return mViewGetter.getWindowDecorViews();
    }

    public boolean waitForDecorViews(long timeout) {
        return mWaiter.waitForWindowDecorViews(timeout);
    }

    /**
     * 点击屏幕上出现的某个符合regex的文本（如果有多个文本出现，则默认只点击屏幕上出现的第一个）;
     * @param regex
     * @return
     */
    public boolean clickOnText(String regex) {
        return mClicker.clickOnText(regex, mConfig.defaultSearchTimeout, true);
    }

    public boolean clickOnTextWithTimeout(String regex,long timeout) {
        TextView textView = mWaiter.waitForTextAndGet(regex, timeout);
        return textView != null && mClicker.clickOnView(textView);
    }

    public boolean longClickOnText(String text) {
        return longClickOnText(text, mConfig.defaultLongClickTime);
    }


    public boolean longClickOnText(String text, long time) {
        return mClicker.longClickOnText(text, mConfig.defaultSearchTimeout, true, time);
    }

    public boolean longClickOnTextWithTimeout(String regex,long time,long timeout) {
        TextView textView = mWaiter.waitForTextAndGet(regex,timeout);
        return textView != null && mClicker.longClickOnView(textView,time);
    }

    public boolean clickOnView(View target) {
        return mClicker.clickOnView(target);
    }

    public boolean clickOnViewById(int id) {
        View target = findViewById(id);
        return target != null && mClicker.clickOnView(target);
    }

    public boolean clickOnViewByIdWithTimeout(int id, long timeout) {
        View target = findViewById(id,timeout);
        return target != null && mClicker.clickOnView(target);
    }

    public boolean longClickOnView(View target, int time) {
        return mClicker.longClickOnView(target, time);
    }

    public boolean longClickOnView(View target) {
        return mClicker.longClickOnView(target, mConfig.defaultLongClickTime);
    }

    public boolean clickOnView(View target, View container) {
        return mClicker.clickOnView(target, container);
    }

    public boolean longClickOnView(View target, View container, int time) {
        return mClicker.longClickOnView(target, container, time);
    }

    public boolean clickOnScreen(float x, float y) {
        return mClicker.clickOnScreen(x, y);
    }

    public boolean longClickOnScreen(float x, float y, int time) {
        return mClicker.longClickOnScreen(x, y, time);
    }

    public void clickOnActionBarHomeButton () {
        mClicker.clickOnActionBarHomeButton();
    }

    public void openMenu() {
        mClicker.openMenu();
    }

    public boolean clickOnMenuItem(String regex,long timeout){
        return mClicker.clickOnMenuItem(regex,timeout);
    }

    /*public void clickBackToActivity(String activityName) {

        while (true) {
            mSleeper.sleep(1500);
            Activity activity = mActivityUtils.getCurrentActivity();
            if (activity != null && !activity.getClass().getName().equals(activityName)) {
                Logger.d("send keycode back");
                mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            } else {
                Logger.d("current activity : " + activityName);
                break;
            }
        }


    }*/

    /**
     * 生成一个侧滑手势
     */
    public void swipeOnScreenEdge() {
        mGesture.swipeOnScreenLeftEdge();
    }

    public void swipeFromLeftToRight() {
        mGesture.swipeFromLeftToRight();
    }

    public void swipeFromRightToLeft() {
        mGesture.swipeFromRightToLeft();
    }

    public boolean waitForText(String regex) {
        return mWaiter.waitForTextAppear(regex);
    }

    public TextView waitForTextAndGet(String regex) {
        return mWaiter.waitForTextAndGet(regex,mConfig.defaultWaitTimeout);
    }

    public boolean waitForTextAndClick(String regex) {
        return clickOnView(waitForTextAndGet(regex));
    }

    /**
     * 等待text出现(至少有一个匹配text的textView出现)
     *
     * @param regex   待匹配的文本
     * @param timeout 超时时间
     * @return true 匹配的文本出现，false 在指定的超时时间里面没有出现
     */
    public boolean waitForText(String regex, long timeout) {
        return mWaiter.waitForTextAppear(regex, timeout);
    }

    public boolean waitForText(String regex,long timeout,boolean scroll){
        return mWaiter.waitForTextAppear(regex,timeout,scroll);
    }

    public boolean waitForButton(String regex, long timeout) {
        return mWaiter.waitForButton(regex, timeout);
    }

    public boolean waitForActivity(String activityName) {
        return mWaiter.waitForActivity(activityName, mConfig.defaultWaitTimeout);
    }

    public boolean waitForActivity(String activityName, int timeout) {
        return mWaiter.waitForActivity(activityName, timeout);
    }

    public void blockWaitForActivity(String activityName, String msg) {
        while (!waitForActivity(activityName, mConfig.defaultWaitTimeout)) {
            Logger.i(msg);
        }
    }

    public boolean waitForDialogToOpen(long timeout, boolean sleepFirst) {
        return mDialogUtils.waitForDialogToOpen(timeout, sleepFirst);
    }

    public boolean waitForDialogToClose(long timeout) {
        return mDialogUtils.waitForDialogToClose(timeout);
    }

    public boolean waitForOnCreate(String activityName) {
        return mActivityUtils.waitForOnCreate(activityName,mConfig.defaultWaitTimeout);
    }

    public boolean waitForOnCreate(String activityName,long timeout) {
        return mActivityUtils.waitForOnCreate(activityName,timeout);
    }

    public boolean waitForOnResume(String activityName) {
        return mActivityUtils.waitForOnResume(activityName,mConfig.defaultWaitTimeout);
    }

    public boolean waitForOnResume(String activityName,long timeout) {
        return mActivityUtils.waitForOnResume(activityName,timeout);
    }

    public boolean waitForOnPause(String activityName) {
        return mActivityUtils.waitForOnPause(activityName,mConfig.defaultWaitTimeout);
    }

    public boolean waitForOnPause(String activityName,long timeout) {
        return mActivityUtils.waitForOnDestroy(activityName,timeout);
    }

    public void addActivityFilter(MyInstrumentation.ActivityFilter filter) {
        mInstrumentation.registerActivityFilter(filter);
    }

    public void removeActivityFilter(MyInstrumentation.ActivityFilter filter) {
        mInstrumentation.removeActivityFilter(filter);
    }

    public View getView(int id) {
        return mSearcher.searchForId(id, mConfig.defaultSearchTimeout, false, true);
    }

    public Activity getCurrentActivity() {
        return mActivityUtils.getCurrentActivity();
    }

    public void finishOpenedActivities() {
        mActivityUtils.finishOpenedActivities();
    }

    public void mockSoftKeyBordSearchButton(EditText editText) throws Exception {

        Class<?> etClass = editText.getClass().getSuperclass();
        Field mEditorField = ReflectUtil.findFieldRecursiveImpl(etClass, "mEditor");
        mEditorField.setAccessible(true);
        Object mEditor = mEditorField.get(editText);
        Class<?> editorClass = mEditor.getClass();
        Field inputContentTypeField = ReflectUtil.findFieldRecursiveImpl(editorClass, "mInputContentType");
        inputContentTypeField.setAccessible(true);
        Object inputContentType = inputContentTypeField.get(mEditor);
        Class<?> inputContentTypeClass = inputContentType.getClass();
        Field onEditorActionListenerField = ReflectUtil.findFieldRecursiveImpl(inputContentTypeClass, "onEditorActionListener");
        onEditorActionListenerField.setAccessible(true);
        Object onEditorActionListener = onEditorActionListenerField.
                get(inputContentType);
        Class<?> onEditorActionListenerClass = onEditorActionListener.getClass();
        Method onEditorActionMethod = onEditorActionListenerClass.getMethod("onEditorAction", TextView.class,
                int.class, KeyEvent.class);
        onEditorActionMethod.invoke(onEditorActionListener, editText, EditorInfo.IME_ACTION_SEARCH, null);
    }

    public void scrollView(View view,int direction){
        mScroller.scrollView(view,direction);
    }


    public Context getContext() {
        return mInstrumentation.getContext();
    }

    public void runOnMainSync(Runnable runnable) {
        mInstrumentation.runOnMainSync(runnable);
    }

    public void goBackToActivity(String activityName) {
        mActivityUtils.goBackToActivity(activityName);
    }

    public void dump(View target,String path) {
        try {
            mParser.dump(target,path);
        } catch (IOException e) {
            Logger.e(null,e);
        }
    }

    public void sleep(long time){
        mSleeper.sleep(time);
    }

    public static class Config {
        /**
         * 超时时间
         */
        public int defaultWaitTimeout = 20 * 1000;
        /**
         * 默认搜索时间
         */
        public long defaultSearchTimeout = 10 * 1000;
        /**
         * 默认长按时间
         */
        public long defaultLongClickTime = 1500;
        /**
         * 默认睡眠的最小时间.
         */
        public int defaultSleepMiniDuration = 300;
        /**
         * 默认睡眠时间
         */
        public int sleepDuration = 500;
    }

}