package io.rtbs.test

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.rettermobile.rbs.RBS
import com.rettermobile.rbs.RBSLogger
import com.rettermobile.rbs.cloud.RBSCallMethodOptions
import com.rettermobile.rbs.cloud.RBSCloudObject
import com.rettermobile.rbs.cloud.RBSGetCloudObjectOptions
import com.rettermobile.rbs.model.RBSClientAuthStatus
import com.rettermobile.rbs.util.RBSRegion
import io.rtbs.test.databinding.ActivityMainBinding
import io.rtbs.test.model.GetProfileResponse
import io.rtbs.test.model.LoginRequest
import io.rtbs.test.model.LoginResponse

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var rbs: RBS

    private var userObject: RBSCloudObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rbs = RBS(
            applicationContext,
            projectId = "2c771eb22f6d4c2d8e70e0f0e8c0bc10",
            region = RBSRegion.EU_WEST_1_BETA
        )

        rbs.setOnClientAuthStatusChangeListener { authStatus, rbsUser ->
            binding.getProfile.isVisible = authStatus == RBSClientAuthStatus.SIGNED_IN
            binding.doLogout.isVisible = authStatus == RBSClientAuthStatus.SIGNED_IN
            binding.doLogin.isVisible = authStatus != RBSClientAuthStatus.SIGNED_IN

            if (authStatus == RBSClientAuthStatus.SIGNED_IN) {
                rbs.getCloudObject(
                    options = RBSGetCloudObjectOptions(
                        classId = "User", instanceId = rbsUser?.uid
                    ),
                    onSuccess = { cloudObj ->
                        userObject = cloudObj

                        userObject?.public?.subscribe(eventFired = {
                            RBSLogger.log("userObject?.public: $it")
                        })
                    }
                )
            }
        }

        binding.doLogout.setOnClickListener { rbs.signOut() }

        binding.doLogin.setOnClickListener {
            rbs.getCloudObject(
                options = RBSGetCloudObjectOptions(
                    classId = "User", key = Pair("username", "loodos")
                ),
                onSuccess = { cloudObj ->
                    cloudObj?.call(
                        options = RBSCallMethodOptions(
                            method = "signin",
                            body = LoginRequest(password = "123123")
                        ),
                        onSuccess = {
                            it?.body<LoginResponse>()?.let { loginRes ->
                                rbs.authenticateWithCustomToken(loginRes.customToken, error = {
                                    val builder = AlertDialog.Builder(this)
                                    builder.setTitle("AuthenticateWithCustomToken Error")
                                    builder.setMessage(it?.message)
                                    builder.setPositiveButton("OK") { dialog, which -> }
                                    builder.show()
                                })
                            }
                        },
                        onError = {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("GetCloudObject Error")
                            builder.setMessage(it?.message)
                            builder.setPositiveButton("OK") { dialog, which -> }
                            builder.show()
                        }
                    )
                }
            )
        }

        binding.getProfile.setOnClickListener {
            userObject?.call(options = RBSCallMethodOptions(method = "getProfile"), onSuccess = {
                it?.body<GetProfileResponse>()?.let { profileRes ->
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Success")
                    builder.setMessage(profileRes.username)
                    builder.setPositiveButton("OK") { dialog, which -> }
                    builder.show()
                }
            }, onError = {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("GetProfile Error")
                builder.setMessage(it?.message)
                builder.setPositiveButton("OK") { dialog, which -> }
                builder.show()
            })
        }
    }
}