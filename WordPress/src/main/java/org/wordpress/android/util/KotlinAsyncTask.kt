package org.wordpress.android.util

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors


abstract class KotlinAsyncTask <Params, Progress, Result> {

    @WorkerThread
    protected abstract fun doInBackground(vararg params: Params) : Result

    @MainThread
    protected open fun onPreExecute() {}

    @MainThread
    protected open fun onPostExecute(result: Result) {}

    @MainThread
    protected open fun onProgressUpdate(vararg value: Progress) {}

    @MainThread
    protected open fun onCancelled(result: Result) {}

    @MainThread
    protected open fun onCancelled() {}

    @Volatile
    private var status: Status = Status.PENDING

    fun getStatus(): Status {
        return status
    }

    enum class Status {
        PENDING, RUNNING, FINISHED
    }

    private var job: Job? = null
    private var scope: CoroutineScope? = null

    private fun executeTask(vararg value: Params, dispatcher: CoroutineDispatcher = Dispatchers.Main){
        var res: Result

        val exceptionHandler = CoroutineExceptionHandler { _, exe ->
            println(":>> EXCEPTION OCCURRED - ${exe.localizedMessage}")
            println(":>> Job canceled - $job")
            this@KotlinAsyncTask.job?.cancel()
            exe.printStackTrace()
        }

        scope = CoroutineScope(dispatcher + exceptionHandler)

        job = scope!!.launch {
            withContext(Dispatchers.Main + exceptionHandler) {

                status = Status.RUNNING
                onPreExecute()

                withContext(Dispatchers.IO + exceptionHandler) {
                    res = doInBackground(*value)
                }

                status = Status.FINISHED
                onPostExecute(res)

                this@KotlinAsyncTask.job?.cancel()
            }
        }
    }

    fun cancel(cancel: Boolean){
        this.cancel(cancel)
    }

    fun cancel(cancel: Boolean, cause: CancellationException = CancellationException()) {
        if(!cancel) return
        this.job?.cancel(cause)
        if (this.job?.isCancelled == true)
            throw cause
    }

    fun isCancelled(): Boolean? {
        return this.job?.isCancelled
    }


    @MainThread
    fun execute(vararg value: Params){
        this.executeTask(*value)
    }

    companion object{
        var THREAD_POOL_COUNT: Int = 20
    }

    @MainThread
    fun executeOnExecutor(vararg value: Params){
        val executorDispatcher = Executors.newFixedThreadPool(THREAD_POOL_COUNT).asCoroutineDispatcher()
        this.executeTask(*value, dispatcher = executorDispatcher)
    }


    fun publishProgress(vararg value: Progress) {
        var publishJob: Job? = null
        publishJob = CoroutineScope(Dispatchers.Main).launch{
            withContext(Dispatchers.Main){
                onProgressUpdate(*value)
                publishJob?.cancel()
            }
        }
    }
}
