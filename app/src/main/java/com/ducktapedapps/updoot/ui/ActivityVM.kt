package com.ducktapedapps.updoot.ui

import android.content.Context
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ducktapedapps.updoot.backgroundWork.enqueueCleanUpWork
import com.ducktapedapps.updoot.backgroundWork.enqueueOneOffSubscriptionsSyncFor
import com.ducktapedapps.updoot.backgroundWork.enqueueSubscriptionSyncWork
import com.ducktapedapps.updoot.data.local.SubredditDAO
import com.ducktapedapps.updoot.data.local.model.Subreddit
import com.ducktapedapps.updoot.ui.User.LoggedIn
import com.ducktapedapps.updoot.ui.User.LoggedOut
import com.ducktapedapps.updoot.ui.navDrawer.AccountModel
import com.ducktapedapps.updoot.ui.navDrawer.AllNavigationEntries
import com.ducktapedapps.updoot.ui.navDrawer.NavigationDestination
import com.ducktapedapps.updoot.utils.Constants
import com.ducktapedapps.updoot.utils.SingleLiveEvent
import com.ducktapedapps.updoot.utils.accountManagement.IRedditClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActivityVM @ViewModelInject constructor(
        @ApplicationContext private val appContext: Context,
        private val redditClient: IRedditClient,
        private val subredditDAO: SubredditDAO
) : ViewModel() {
    private val _shouldReload = MutableStateFlow(SingleLiveEvent(false))
    val shouldReload: StateFlow<SingleLiveEvent<Boolean>> = _shouldReload

    private val _accounts: MutableStateFlow<List<AccountModel>> = MutableStateFlow(redditClient.getAccountModels())
    private val accountEntriesExpanded = MutableStateFlow(false)

    private val _navDrawerVisible = MutableLiveData(true)
    val navDrawerVisibility = _navDrawerVisible

    val user: Flow<User> = _accounts.map {
        if (it.first().name == Constants.ANON_USER) LoggedOut
        else LoggedIn(it.first().name)
    }

    val accounts: Flow<List<AccountModel>> = accountEntriesExpanded.combine(_accounts) { expanded, accounts: List<AccountModel> ->
        if (expanded) accounts
        else listOf(accounts.first())
    }

    private fun reloadAccountList() {
        _accounts.value = redditClient.getAccountModels()
    }

    fun setCurrentAccount(name: String) {
        redditClient.setCurrentAccount(name)
        _shouldReload.value = SingleLiveEvent(true)
        reloadAccountList()
        collapseAccountsMenu()
    }

    fun reloadContent() {
        _shouldReload.value = SingleLiveEvent(true)
        reloadAccountList()
        collapseAccountsMenu()
    }

    fun logout(accountName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val accountRemovedSuccessfully = redditClient.removeUser(accountName)
            if (accountRemovedSuccessfully) withContext(Dispatchers.Main) {
                reloadContent()
                reloadAccountList()
                collapseAccountsMenu()
            }
        }
    }

    private fun collapseAccountsMenu() {
        accountEntriesExpanded.value = false
    }

    fun toggleAccountsMenuList() {
        accountEntriesExpanded.value = !accountEntriesExpanded.value
    }

    fun showBottomNavDrawer() {
        _navDrawerVisible.value = true
    }

    fun hideBottomNavDrawer() {
        _navDrawerVisible.value = false
    }

    val navigationEntries: Flow<List<NavigationDestination>> = combine(flow { emit(AllNavigationEntries) }, user) { allEntries, loginState ->
        when (loginState) {
            is LoggedOut -> allEntries.filterNot { it.isUserSpecific }
            is LoggedIn -> allEntries
        }
    }

    private val query: MutableStateFlow<String> = MutableStateFlow("")
    private val _searchQueryLoading = MutableStateFlow(false)
    val searchQueryLoading: StateFlow<Boolean> = _searchQueryLoading
    private var currentSearchJob: Job? = null

    val results: Flow<List<Subreddit>> = query.combineTransform(user) { keyWord: String, user: User ->
        if (keyWord.isNotBlank()) emit(subredditDAO.observeSubredditWithKeyword(keyWord).distinctUntilChanged())
        else emit(subredditDAO.observeSubscribedSubredditsFor(user.name).distinctUntilChanged())
    }.flattenMerge()

    fun searchSubreddit(queryString: String) {
        currentSearchJob?.cancel()
        currentSearchJob = viewModelScope.launch(Dispatchers.IO) {
            query.value = queryString
            if (queryString.isNotBlank())
                try {
                    _searchQueryLoading.value = true
                    val redditAPI = redditClient.api()
                    val results = redditAPI.search(query = queryString)
                    results.children.forEach { subreddit -> subredditDAO.insertSubreddit(subreddit) }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                } finally {
                    _searchQueryLoading.value = false
                }
        }
    }

    fun enqueueSubscriptionSyncWork() {
        val currentLoggedIn = _accounts.value.first()
        appContext.enqueueOneOffSubscriptionsSyncFor(currentLoggedIn.name)
    }

    private fun enqueuePeriodicWork() {
        appContext.apply {
            enqueueSubscriptionSyncWork()
            enqueueCleanUpWork()
        }
    }

    override fun onCleared() {
        super.onCleared()
        enqueuePeriodicWork()
    }
}

sealed class User(val name: String) {
    object LoggedOut : User(Constants.ANON_USER)
    data class LoggedIn(val userName: String) : User(userName)
}