package workmeter.orange.ebin.com.dogapp.screens.login

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import workmeter.orange.ebin.com.dogapp.R

import kotlinx.android.synthetic.main.activity_login.*
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.support.annotation.UiThread
import android.support.design.widget.TextInputEditText
import com.dd.CircularProgressButton
import retrofit2.Call
import retrofit2.Retrofit
import workmeter.orange.ebin.com.dogapp.appication.DogApp
import workmeter.orange.ebin.com.dogapp.screens.login.api.service.GetWorkMeterDataService
import workmeter.orange.ebin.com.dogapp.screens.login.component.DaggerLoginActivityComponent
import workmeter.orange.ebin.com.dogapp.screens.login.model.Workmeter
import workmeter.orange.ebin.com.dogapp.screens.login.module.LoginActivityModule
import workmeter.orange.ebin.com.dogapp.utils.FieldValidator
import javax.inject.Inject
import javax.inject.Named
import retrofit2.Callback
import retrofit2.Response
import workmeter.orange.ebin.com.dogapp.database.DBHandler
import workmeter.orange.ebin.com.dogapp.screens.home.HomeActivity


class LoginActivity : AppCompatActivity() {
    lateinit var circularProgressLogin: CircularProgressButton

    @Inject
    @field:Named("orangeservice_retrofit") lateinit var retrofitOrangeService: Retrofit

    @Inject
    lateinit var sharedPreferences : SharedPreferences
    @Inject
    lateinit var dbHandler : DBHandler

    var isEnableLoginButton :Boolean = true
    lateinit var textInputEditText :TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        //AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setSupportActionBar(toolbar)
        supportActionBar?.hide()
        var loginActivityComponent = DaggerLoginActivityComponent.builder()
                .loginActivityModule(LoginActivityModule(this))
                .dogAppComponent(DogApp.get(this).getOrangAppComponent())
                .build()
        loginActivityComponent.inject(this)
        linkUI()
        takeEvents()
    }

    private fun takeEvents() {
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Get workmeter key from HRM site", Snackbar.LENGTH_LONG)
                    .setAction("OPEN", { view ->
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://128.199.70.181/hris/symfony/web/index.php/pim/viewMyDetails"))
                        startActivity(browserIntent)
                    }).show()
        }
        circularProgressLogin.setOnClickListener({ view ->
//            if (circularProgressLogin.getProgress() === 0) {
            if(isEnableLoginButton) {
                loading()
                requestForWorkMeterData()
            }else{

            }
        })
    }



    private fun requestForWorkMeterData() {
        if(FieldValidator.isValidFieldEntry(textInputEditText)){
            val service = retrofitOrangeService.create(GetWorkMeterDataService::class.java)
            var call : Call<Workmeter> = service.getWorkMeterDataService(textInputEditText.text.toString())
            call.enqueue(object : Callback<Workmeter> {
                override fun onResponse(call: Call<Workmeter>?, response: Response<Workmeter>?) {
                    if(response?.body()?.empName != null){
                        loadingFinished()
                        updatePreference(response.body()?.empKey)
                        savetoDb(response.body()!!);
                        var intent = Intent(this@LoginActivity, HomeActivity::class.java)
                        Handler().postDelayed({
                            startActivity(intent)
                            this@LoginActivity.finish()
                        }, 1000)


                    }else loadingFailed("Incorrect key!")
                }
                override fun onFailure(call: Call<Workmeter>?, t: Throwable?) {
                    loadingFailed("Can't connect to server")
                }
            })
        }else {
            loadingFailed("")
            textInputEditText.error = "Enter a valid key"
              }
    }

    private fun savetoDb(body: Workmeter) {
//        var asa = Workmeter::class.java.declaredFields
        dbHandler.addWorkMeterEntry(body)

    }

    private fun updatePreference(empKey: String?) {
        sharedPreferences.edit().putString("key",empKey).commit()
    }

    @UiThread
    private fun loadingFinished() {
//        textInputEditText.isEnabled = true
//        isEnableLoginButton = true
        circularProgressLogin.progress = 100
    }

    @UiThread
    private fun loading() {
        textInputEditText.isEnabled = false
        isEnableLoginButton = false
        circularProgressLogin.progress = 50
    }

    @UiThread
    private fun loadingFailed(message: String) {
        if(message.length>0){
            Snackbar.make(textInputEditText, message, Snackbar.LENGTH_LONG)
                    .setAction("",null).show()
        }
        textInputEditText.isEnabled = true
        isEnableLoginButton = true
        circularProgressLogin.progress = 0
    }


    private fun linkUI() {
        textInputEditText = findViewById(R.id.textInputEditText)
        circularProgressLogin = findViewById(R.id.circularButtonLogin)
        circularProgressLogin.isIndeterminateProgressMode = true
    }

}