package com.github.osuCarl

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.utils.info
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.ConsoleCommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.PluginDataExtensions.mapKeys
import net.mamoe.mirai.console.data.PluginDataExtensions.withEmptyDefault
import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.scopeWith
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.info
import java.io.File
import kotlin.random.Random

object MiraiConsoleTaroDice : KotlinPlugin(
    JvmPluginDescription(
        id = "com.github.osuCarl.mirai-console-taroDice",
        name = "mirai-console-taroDice",
        version = "0.1.0",
    ) {
        author("com.github.osuCarl")
    }
) {
    val PERMISSION_EXECUTE_1 by lazy {
        PermissionService.INSTANCE.register(permissionId("execute1"), "注册权限的示例")
    }

    val PERMISSION_EXECUTE_2 by lazy {
        PermissionService.INSTANCE.register(permissionId("execute2"), "给蜂蜂小甜心的权限")
    }

    override fun onEnable() {
        MySetting.reload() // 从数据库自动读取配置实例
        MyPluginData.reload()

        logger.info { "Hi: ${MySetting.name}" } // 输出一条日志.
//        logger.info("Hi: ${MySetting.name}") // 输出一条日志. 与上面一条相同, 但更推荐上面一条.
//        logger.verbose("Hi: ${MySetting.name}") // 多种日志级别可选

        // 请不要使用 println, System.out.println 等标准输出方式. 请总是使用 logger.

        MySimpleCommand.register() // 注册指令

        PERMISSION_EXECUTE_1 // 初始化, 注册权限
        PERMISSION_EXECUTE_2
    }

    override fun onDisable() {
        MySimpleCommand.unregister() // 取消注册指令
    }
}

// 定义插件数据
// 插件
object MyPluginData : AutoSavePluginData("TaroDiceData") { // "TaroDiceData" 是保存的文件名 (不带后缀)
    var list: MutableList<String> by value(mutableListOf("a", "b")) // mutableListOf("a", "b") 是初始值, 可以省略
    var long: Long by value(0L) // 允许 var
    var int by value(0) // 可以使用类型推断, 但更推荐使用 `var long: Long by value(0)` 这种定义方式.


    // 带默认值的非空 map.
    // notnullMap[1] 的返回值总是非 null 的 MutableMap<Int, String>
    var notnullMap
            by value<MutableMap<Int, MutableMap<Int, String>>>().withEmptyDefault()

    // 可将 MutableMap<Long, Long> 映射到 MutableMap<Bot, Long>.
    val botToLongMap: MutableMap<Bot, Long> by value<MutableMap<Long, Long>>().mapKeys(Bot::getInstance, Bot::id)
}

// 定义一个配置. 所有属性都会被追踪修改, 并自动保存.
// 配置是插件与用户交互的接口, 但不能用来保存插件的数据.
object MySetting : ReadOnlyPluginConfig("TaroDiceSetting") { // "TaroDiceSetting" 是保存的文件名 (不带后缀)
    val name by value("test")

    @ValueDescription("数量") // 注释, 将会保存在 TaroDiceSetting.yml 文件中.
    val count by value(0)

    val nested by value<MyNestedData>() // 嵌套类型是支持的
}

@Serializable
data class MyNestedData(
    val list: List<String> = listOf()
)

// Define the data class for Tarot card entries
@Serializable
data class TarotData(
    val taro: List<String>
)

// 简单指令
object MySimpleCommand : SimpleCommand(
    MiraiConsoleTaroDice, "dice",
    description = "示例指令"
) {
    // 会自动创建一个 ID 为 "com.github.osuCarl.mirai-console-taroDice:command.dice" 的权限.
    private val tarotDescriptions: List<String> by lazy {
        val jsonpath = "data/com.github.osuCarl.mirai-console-taroDice/taroCard.json"
        val fileContent = File(jsonpath).readText()
        val tarotData = Json.decodeFromString<TarotData>(fileContent)
        tarotData.taro
    }

    private val imagePattern = """\[CQ:image,file=([^\]]+)]""".toRegex()

    // 通过 /dice 调用, 参数自动解析
    @Handler
    suspend fun CommandSender.handle() {
        if (this.hasPermission(MiraiConsoleTaroDice.PERMISSION_EXECUTE_2)) {
            //todo

        } else if (this.hasPermission(MiraiConsoleTaroDice.PERMISSION_EXECUTE_1)) {
            // Select a random Tarot card description
            val randomDescription = tarotDescriptions[Random.nextInt(tarotDescriptions.size)]
//            sendMessage(randomDescription)
            // Find the image file path within the description
            val imageMatch = imagePattern.find(randomDescription)
            val imagePath = imageMatch?.groups?.get(1)?.value
            val descriptionWithoutImage = imagePattern.replace(randomDescription, "").trim()

            // Send the description without the image tag
            sendMessage("随机抽取的塔罗牌描述是：\n$descriptionWithoutImage")

            // Send the image if the path was found
            if (imagePath != null) {
                val imageFile = File("data/com.github.osuCarl.mirai-console-taroDice/image/$imagePath") // image directory
                if (imageFile.exists()) {
                    val imageResource = imageFile.toExternalResource("jpg")
                    val image = subject!!.uploadImage(imageResource)
                    imageResource.close()
                    sendMessage(image)
//                    sendMessage("这是一张图片：$imagePath")
                } else {
                    sendMessage("未找到图像文件：$imagePath")
                }
            }
        } else {
            sendMessage("你没有抽取塔罗牌的权限，联系蜂蜂小甜心给你添加权限")
        }
    }
}

