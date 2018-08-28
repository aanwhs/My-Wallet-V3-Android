package com.blockchain.kycui.profile

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.NabuApiException
import com.blockchain.kyc.models.nabu.NabuErrorCodes
import com.blockchain.kyc.util.toISO8601DateString
import com.blockchain.kycui.profile.models.ProfileModel
import com.blockchain.nabu.metadata.NabuCredentialsMetadata.Companion.USER_CREDENTIALS_METADATA_NODE
import com.blockchain.nabu.models.NabuOfflineTokenResponse
import com.blockchain.nabu.models.mapToMetadata
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.kyc.R
import timber.log.Timber
import kotlin.properties.Delegates

class KycProfilePresenter(
    private val nabuDataManager: NabuDataManager,
    private val metadataManager: MetadataManager
) : BasePresenter<KycProfileView>() {

    var firstNameSet by Delegates.observable(false) { _, _, _ -> enableButtonIfComplete() }
    var lastNameSet by Delegates.observable(false) { _, _, _ -> enableButtonIfComplete() }
    var dateSet by Delegates.observable(false) { _, _, _ -> enableButtonIfComplete() }

    override fun onViewReady() = Unit

    internal fun onContinueClicked() {
        check(!view.firstName.isEmpty()) { "firstName is empty" }
        check(!view.lastName.isEmpty()) { "lastName is empty" }
        check(view.dateOfBirth != null) { "dateOfBirth is null" }

        compositeDisposable +=
            metadataManager.fetchMetadata(USER_CREDENTIALS_METADATA_NODE)
                .subscribeOn(Schedulers.io())
                .flatMapCompletable { optionalToken ->
                    if (optionalToken.isPresent) {
                        // Here we assume that the user was already created, otherwise metadata wouldn't
                        // have been stored.
                        Completable.complete()
                    } else {
                        createUserAndStoreInMetadata()
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view.showProgressDialog() }
                .doOnTerminate { view.dismissProgressDialog() }
                .doOnError(Timber::e)
                .subscribeBy(
                    onComplete = {
                        ProfileModel(
                            view.firstName,
                            view.lastName,
                            view.dateOfBirth ?: throw IllegalStateException("DoB has not been set"),
                            view.countryCode
                        ).run { view.continueSignUp(this) }
                    },
                    onError = {
                        if (it is NabuApiException &&
                            it.getErrorCode() == NabuErrorCodes.AlreadyRegistered
                        ) {
                            view.showErrorToast(R.string.kyc_profile_error_conflict)
                        } else {
                            view.showErrorToast(R.string.kyc_profile_error)
                        }
                    }
                )
    }

    private fun createUserAndStoreInMetadata(): Completable = nabuDataManager.createUser()
        .subscribeOn(Schedulers.io())
        .flatMapCompletable { jwt ->
            nabuDataManager.getAuthToken(jwt)
                .subscribeOn(Schedulers.io())
                .flatMapCompletable { tokenResponse ->
                    metadataManager.saveToMetadata(tokenResponse.mapToMetadata())
                        .toSingle { tokenResponse }
                        .flatMapCompletable { createBasicUser(it) }
                }
        }

    private fun createBasicUser(offlineToken: NabuOfflineTokenResponse): Completable =
        nabuDataManager.createBasicUser(
            view.firstName,
            view.lastName,
            view.dateOfBirth?.toISO8601DateString()
                ?: throw IllegalStateException("DoB has not been set"),
            offlineToken
        ).subscribeOn(Schedulers.io())

    private fun enableButtonIfComplete() {
        view.setButtonEnabled(firstNameSet && lastNameSet && dateSet)
    }

    internal fun onProgressCancelled() {
        compositeDisposable.clear()
    }
}