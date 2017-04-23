package tk.woppo.mgame;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Scroller;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import tk.woppo.mgame.tools.MyApplication;

/**
 * 点击事件
 */
public class InputListener implements View.OnTouchListener {

    private static final int SWIPE_MIN_DISTANCE = 0;
    private static final int SWIPE_THRESHOLD_VELOCITY = 25;
    private static final int MOVE_THRESHOLD = 250;
    private static final int RESET_STARTING = 10;

    private float x;
    private float y;
    private float lastdx;
    private float lastdy;
    private float previousX;
    private float previousY;
    private float startingX;
    private float startingY;
    private int previousDirection = 1;
    private int veryLastDirection = 1;
    private boolean hasMoved = false;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private Activity activity;
    private AlertDialog.Builder builder;
    private AlertDialog dialog;
    private ImageView iv_quit;
    private ListView lv_rank;
    private SimpleAdapter adapter;
    private Dialog progressDialog;
    private String[] user,score;
    MainView mView;

    public InputListener(MainView view, Activity activity) {
        super();
        this.mView = view;
        this.activity = activity;

    }

    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                x = event.getX();
                y = event.getY();
                startingX = x;
                startingY = y;
                previousX = x;
                previousY = y;
                lastdx = 0;
                lastdy = 0;
                hasMoved = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                x = event.getX();
                y = event.getY();
                if (mView.game.isActive()) {
                    float dx = x - previousX;
                    if (Math.abs(lastdx + dx) < Math.abs(lastdx) + Math.abs(dx) && Math.abs(dx) > RESET_STARTING
                            && Math.abs(x - startingX) > SWIPE_MIN_DISTANCE) {
                        startingX = x;
                        startingY = y;
                        lastdx = dx;
                        previousDirection = veryLastDirection;
                    }
                    if (lastdx == 0) {
                        lastdx = dx;
                    }
                    float dy = y - previousY;
                    if (Math.abs(lastdy + dy) < Math.abs(lastdy) + Math.abs(dy) && Math.abs(dy) > RESET_STARTING
                            && Math.abs(y - startingY) > SWIPE_MIN_DISTANCE) {
                        startingX = x;
                        startingY = y;
                        lastdy = dy;
                        previousDirection = veryLastDirection;
                    }
                    if (lastdy == 0) {
                        lastdy = dy;
                    }
                    if (pathMoved() > SWIPE_MIN_DISTANCE * SWIPE_MIN_DISTANCE && !hasMoved) {
                        boolean moved = false;
                        //Vertical
                        if (((dy >= SWIPE_THRESHOLD_VELOCITY && Math.abs(dy) >= Math.abs(dx)) || y - startingY >= MOVE_THRESHOLD) && previousDirection % 2 != 0) {
                            moved = true;
                            previousDirection = previousDirection * 2;
                            veryLastDirection = 2;
                            mView.game.move(2);
                        } else if (((dy <= -SWIPE_THRESHOLD_VELOCITY && Math.abs(dy) >= Math.abs(dx)) || y - startingY <= -MOVE_THRESHOLD) && previousDirection % 3 != 0) {
                            moved = true;
                            previousDirection = previousDirection * 3;
                            veryLastDirection = 3;
                            mView.game.move(0);
                        }
                        //Horizontal
                        if (((dx >= SWIPE_THRESHOLD_VELOCITY && Math.abs(dx) >= Math.abs(dy)) || x - startingX >= MOVE_THRESHOLD) && previousDirection % 5 != 0) {
                            moved = true;
                            previousDirection = previousDirection * 5;
                            veryLastDirection = 5;
                            mView.game.move(1);
                        } else if (((dx <= -SWIPE_THRESHOLD_VELOCITY && Math.abs(dx) >= Math.abs(dy)) || x - startingX <= -MOVE_THRESHOLD) && previousDirection % 7 != 0) {
                            moved = true;
                            previousDirection = previousDirection * 7;
                            veryLastDirection = 7;
                            mView.game.move(3);
                        }
                        if (moved) {
                            hasMoved = true;
                            startingX = x;
                            startingY = y;
                        }
                    }
                }
                previousX = x;
                previousY = y;
                return true;
            case MotionEvent.ACTION_UP:
                x = event.getX();
                y = event.getY();
                previousDirection = 1;
                veryLastDirection = 1;
                //"Menu" inputs
                if (!hasMoved) {
                    if (iconPressed(mView.sXNewGame, mView.sYIcons)) {
                        mView.game.newGame();
                    } else if (iconPressed(mView.sXUndo, mView.sYIcons)) {
                        mView.game.revertUndoState();
                    } else if (isTap(2) && inRange(mView.startingX, x, mView.endingX)
                            && inRange(mView.startingY, x, mView.endingY) && mView.continueButtonEnabled) {
                        mView.game.setEndlessMode();
                    } else if (iconPressed(mView.sRank, mView.sYIcons)) {
                        init();
                    }
                }
        }
        return true;
    }
    //布局初始化
    private void init() {
        builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.rank_dialog, null);
        iv_quit = (ImageView) view.findViewById(R.id.iv_quit);
        iv_quit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                dialog = null;
                builder = null;
            }
        });
        lv_rank = (ListView) view.findViewById(R.id.lv_rank);
        lv_rank.setEnabled(false);
        builder.setView(view);
        builder.setCancelable(false);
        dialog = builder.show();
        initDialog();
        http();
    }

    //联网请求
    private void http() {
        OkHttpClient client = new OkHttpClient();
        RequestBody requestbody = RequestBody.create(JSON, "");
        final Request request = new Request.Builder().
                url(activity.getResources().getString(R.string.getrank)).
                post(requestbody).
                build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(activity, "排行信息获取失败，请检查网络", Toast.LENGTH_SHORT).show();
                handler.sendEmptyMessage(0);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String str = response.body().string();
                Log.e("result",str);
                try {
                    JSONArray array = new JSONArray(str);
                    user = new String[array.length()];
                    score = new String[array.length()];
                    for (int i =0;i<array.length();i++){
                        JSONObject object = new JSONObject(array.get(i).toString());
                        user[i] = object.getString("user");
                        score[i] = object.getString("score");
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                handler.sendEmptyMessage(1);
            }
        });
    }
    //用户数据
    private List<Map<String, Object>> getdata() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map;
        map = new HashMap<String, Object>();
        map.put("user","用户名");
        map.put("score", "score");
        list.add(map);
        for (int i = 0; i < user.length; i++) {
            map = new HashMap<String, Object>();
            map.put("user",user[i]);
            map.put("score",score[i]);
            list.add(map);
        }
        return list;
    }


    //等待框
    private void initDialog() {
        progressDialog = new Dialog(activity, R.style.progress_dialog);
        progressDialog.setContentView(R.layout.dialog);
        progressDialog.setCancelable(true);
        progressDialog.setCancelable(false);
        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        TextView msg = (TextView) progressDialog.findViewById(R.id.id_tv_loadingmsg);
        msg.setText("获取中");
        progressDialog.show();
    }
    //UI更新
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    Toast.makeText(activity, "获取列表失败,请检测网络", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    adapter = new SimpleAdapter(activity, getdata(), R.layout.rank_dialog_item,
                            new String[]{"user", "score"},
                            new int[]{R.id.tv_user, R.id.tv_score});
                    lv_rank.setAdapter(adapter);
                    break;
            }
            progressDialog.dismiss();
        }
    };

    private float pathMoved() {
        return (x - startingX) * (x - startingX) + (y - startingY) * (y - startingY);
    }

    private boolean iconPressed(int sx, int sy) {
        return isTap(1) && inRange(sx, x, sx + mView.iconSize)
                && inRange(sy, y, sy + mView.iconSize);
    }

    private boolean inRange(float starting, float check, float ending) {
        return (starting <= check && check <= ending);
    }

    private boolean isTap(int factor) {
        return pathMoved() <= mView.iconSize * factor;
    }


}
