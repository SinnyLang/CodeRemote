package xyz.sl.coderemote

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

import xyz.sl.coderemote.ui.StartProjectActivity
import xyz.sl.coderemote.utils.EditFileManger

class MainActivity : ComponentActivity() {

    companion object {
        lateinit var editFileManger: EditFileManger
            private set

        // !!! NOTE: this method is called only in Ui Preview
        fun setEditFileMangerForUiPreview(){
            editFileManger = EditFileManger(null)
        }
        fun setEditFileMangerForUiPreview(context: Context){
            editFileManger = EditFileManger(context)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 创建全局 EditFileManger 对象，用于管理文件
        editFileManger = EditFileManger(this.application.applicationContext)

        // 2. 启动 App 程序
        startActivity(Intent(this, StartProjectActivity::class.java))
        finish()
    }


}


