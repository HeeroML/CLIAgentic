package com.cliagentic.mobileterminal.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cliagentic.mobileterminal.CliAgenticApp
import com.cliagentic.mobileterminal.data.model.AuthType
import com.cliagentic.mobileterminal.data.model.WatchRuleType
import com.cliagentic.mobileterminal.di.AppContainer
import com.cliagentic.mobileterminal.ui.screens.PrivacyScreen
import com.cliagentic.mobileterminal.ui.screens.ProfileEditorScreen
import com.cliagentic.mobileterminal.ui.screens.ProfilesScreen
import com.cliagentic.mobileterminal.ui.screens.SessionScreen
import com.cliagentic.mobileterminal.ui.screens.SettingsScreen
import com.cliagentic.mobileterminal.ui.viewmodel.ProfileEditorViewModel
import com.cliagentic.mobileterminal.ui.viewmodel.ProfilesViewModel
import com.cliagentic.mobileterminal.ui.viewmodel.SessionViewModel
import com.cliagentic.mobileterminal.ui.viewmodel.SettingsViewModel
import com.cliagentic.mobileterminal.voice.DictationEngine
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    appContainer: AppContainer
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoute.Profiles.route,
        modifier = modifier
    ) {
        composable(AppRoute.Profiles.route) {
            val vm: ProfilesViewModel = viewModel(
                factory = ProfilesViewModel.factory(appContainer.profileRepository)
            )
            val state by vm.uiState.collectAsStateWithLifecycle()

            ProfilesScreen(
                state = state,
                onAddProfile = { navController.navigate(AppRoute.ProfileEditor.create(null)) },
                onEditProfile = { navController.navigate(AppRoute.ProfileEditor.create(it)) },
                onConnect = { navController.navigate(AppRoute.Session.create(it)) },
                onDelete = vm::deleteProfile,
                onSettings = { navController.navigate(AppRoute.Settings.route) }
            )
        }

        composable(
            route = AppRoute.ProfileEditor.route,
            arguments = listOf(
                navArgument("profileId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getLong("profileId") ?: -1L
            val profileId = rawId.takeIf { it > 0 }

            val vm: ProfileEditorViewModel = viewModel(
                factory = ProfileEditorViewModel.factory(appContainer.profileRepository, profileId)
            )
            val state by vm.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(state.savedProfileId) {
                if (state.savedProfileId != null) {
                    vm.consumeSavedProfile()
                    navController.popBackStack()
                }
            }

            ProfileEditorScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onNameChange = vm::onNameChange,
                onHostChange = vm::onHostChange,
                onPortChange = vm::onPortChange,
                onUsernameChange = vm::onUsernameChange,
                onAuthTypeChange = vm::onAuthTypeChange,
                onBiometricToggle = vm::onBiometricToggle,
                onTmuxPrefixChange = vm::onTmuxPrefixChange,
                onPasswordChange = vm::onPasswordChange,
                onPrivateKeyChange = vm::onPrivateKeyChange,
                onSave = vm::saveProfile
            )
        }

        composable(
            route = AppRoute.Session.route,
            arguments = listOf(navArgument("profileId") { type = NavType.LongType })
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getLong("profileId") ?: return@composable

            val vm: SessionViewModel = viewModel(
                key = "session-$profileId",
                factory = SessionViewModel.factory(
                    profileId = profileId,
                    profileRepository = appContainer.profileRepository,
                    settingsRepository = appContainer.settingsRepository,
                    sshClient = appContainer.sshClient,
                    notificationManager = appContainer.watchNotificationManager
                )
            )

            val state by vm.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val activity = context as? FragmentActivity
            val coroutineScope = rememberCoroutineScope()

            val engine = remember(state.dictationEngineType) {
                appContainer.createDictationEngine(state.dictationEngineType)
            }

            DisposableEffect(engine) {
                onDispose { engine.release() }
            }

            val recordAudioLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    vm.setDictating(true)
                    engine.start(engineListener(vm))
                } else {
                    vm.onDictationPreviewChange("Microphone permission denied")
                }
            }

            val postNotificationLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { }

            val canNotify = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!canNotify) {
                LaunchedEffect(Unit) {
                    postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            SessionScreen(
                state = state,
                onBack = {
                    vm.disconnect()
                    navController.popBackStack()
                },
                onConnect = {
                    val profile = state.profile
                    if (profile?.authType == AuthType.KEY && profile.biometricForKey) {
                        if (activity != null) {
                            coroutineScope.launch {
                                val ok = appContainer.biometricAuthenticator.authenticate(
                                    activity = activity,
                                    title = "Unlock private key",
                                    subtitle = "Authenticate to use the SSH private key"
                                )
                                if (ok) {
                                    vm.connect(biometricUnlocked = true)
                                } else {
                                    vm.clearError()
                                }
                            }
                        } else {
                            vm.connect(biometricUnlocked = false)
                        }
                    } else {
                        vm.connect(biometricUnlocked = false)
                    }
                },
                onDisconnect = vm::disconnect,
                onInputDraftChange = vm::onInputDraftChange,
                onSendDraft = vm::sendDraft,
                onSendLiteral = vm::sendLiteral,
                onSendBytes = vm::sendBytes,
                onToggleCtrl = vm::toggleCtrlArmed,
                onKeepScreenOnChange = vm::setKeepScreenOn,
                onWatchPatternInputChange = vm::onWatchPatternInputChange,
                onWatchTypeChange = vm::onWatchTypeChange,
                onAddWatchRule = vm::addWatchRule,
                onRemoveWatchRule = vm::removeWatchRule,
                onClearWatchRules = vm::clearWatchRules,
                onClearMatchLog = vm::clearMatchLog,
                onHostKeyDecision = vm::resolveHostKeyPrompt,
                onTmuxSessionSelected = vm::attachSelectedTmuxSession,
                onCreateTmuxSession = vm::createAndAttachTmuxSession,
                onDismissTmuxSelector = vm::dismissTmuxSessionSelector,
                onClearError = vm::clearError,
                onClearInfo = vm::clearInfo,
                onClearTerminal = vm::clearTerminal,
                onDictationPreviewChange = vm::onDictationPreviewChange,
                onStartDictation = {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        vm.setDictating(true)
                        engine.start(engineListener(vm))
                    } else {
                        recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopDictation = {
                    vm.setDictating(false)
                    engine.stop()
                },
                onSendDictation = vm::sendDictation
            )
        }

        composable(AppRoute.Settings.route) {
            val vm: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(
                    profileRepository = appContainer.profileRepository,
                    settingsRepository = appContainer.settingsRepository
                )
            )
            val state by vm.uiState.collectAsStateWithLifecycle()

            SettingsScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onVoiceAppendNewlineChange = vm::setVoiceAppendNewline,
                onDictationEngineChange = vm::setDictationEngine,
                onMoshFeatureFlagChange = vm::setMoshFeatureFlag,
                onExport = vm::exportJson,
                onImportJsonChange = vm::onImportJsonChange,
                onImport = vm::importJson,
                onOpenPrivacy = { navController.navigate(AppRoute.Privacy.route) },
                onClearStatus = vm::clearStatus
            )
        }

        composable(AppRoute.Privacy.route) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }
    }
}

private fun engineListener(vm: SessionViewModel): DictationEngine.Listener {
    return object : DictationEngine.Listener {
        override fun onPartial(text: String) {
            vm.onDictationPreviewChange(text)
        }

        override fun onFinal(text: String) {
            vm.setDictating(false)
            vm.onDictationPreviewChange(text)
        }

        override fun onError(message: String) {
            vm.setDictating(false)
            vm.onDictationPreviewChange(message)
        }
    }
}
