package xyz.sl.coderemote

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import xyz.sl.coderemote.core.vfs.LocalResourceProvider
import xyz.sl.coderemote.core.vfs.ResourceManager
import xyz.sl.coderemote.core.vfs.ResourceProvider
import xyz.sl.coderemote.core.vfs.SftpResourceProvider

import xyz.sl.coderemote.ui.StartProjectActivity
import xyz.sl.coderemote.utils.BaseFileManager
import xyz.sl.coderemote.utils.LocalFileManger
import xyz.sl.coderemote.utils.SshClient

class MainActivity : ComponentActivity() {

    companion object {
        lateinit var fileManger: BaseFileManager
            private set

        // !!! NOTE: this method is called only in Ui Preview
        fun setEditFileMangerForUiPreview(){
            fileManger = LocalFileManger(null)
        }
        fun setEditFileMangerForUiPreview(context: Context){
            fileManger = LocalFileManger(context)
        }

        fun setFileManger(fileManger: BaseFileManager){
            Companion.fileManger = fileManger
        }

        // 每次只能有一个主机上的project被打开
        var remoteClient: SshClient? = null

        lateinit var resourceManager: ResourceManager
            private set
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 创建全局 EditFileManger 对象，用于管理文件
        fileManger = LocalFileManger(this.application.applicationContext)

        // 1. 创建 VFS
        resourceManager = ResourceManager(listOf<ResourceProvider>(
            LocalResourceProvider(applicationContext),
            SftpResourceProvider()
        ))

        // 2. 启动 App 程序
        startActivity(Intent(this, StartProjectActivity::class.java))
        finish()
    }


}


