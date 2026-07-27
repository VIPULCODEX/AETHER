package com.aether.android.ui.gym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aether.core.data.AetherRepository
import com.aether.core.engine.ActivityLevel
import com.aether.core.engine.BodyGoal
import com.aether.core.engine.NutritionEngine
import com.aether.core.engine.NutritionPlan
import com.aether.core.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class GymUiState(
    val gymIsFocusArea: Boolean = false,
    val profile: UserProfile? = null,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val bodyGoal: BodyGoal = BodyGoal.MAINTAIN,
    val plan: NutritionPlan? = null
)

class GymViewModel(private val repository: AetherRepository) : ViewModel() {

    private val nutritionEngine = NutritionEngine()

    private val _uiState = MutableStateFlow(GymUiState())
    val uiState: StateFlow<GymUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeFocusAreas(),
                repository.observeUserProfile()
            ) { focusAreas, profile -> focusAreas.contains("Gym") to profile }
                .collect { (gymIsFocusArea, profile) ->
                    val current = _uiState.value
                    val plan = profile?.let { nutritionEngine.compute(it, current.activityLevel, current.bodyGoal) }
                    _uiState.value = current.copy(
                        gymIsFocusArea = gymIsFocusArea,
                        profile = profile,
                        plan = plan
                    )
                }
        }
    }

    fun updateActivityLevel(level: ActivityLevel) {
        recompute(activityLevel = level)
    }

    fun updateBodyGoal(goal: BodyGoal) {
        recompute(bodyGoal = goal)
    }

    fun saveProfile(heightCm: Double, weightKg: Double, age: Int, isMale: Boolean) {
        viewModelScope.launch {
            repository.saveUserProfile(
                UserProfile(
                    heightCm = heightCm,
                    weightKg = weightKg,
                    age = age,
                    isMale = isMale,
                    activityLevel = _uiState.value.activityLevel.name,
                    bodyGoal = _uiState.value.bodyGoal.name
                )
            )
        }
    }

    private fun recompute(
        activityLevel: ActivityLevel = _uiState.value.activityLevel,
        bodyGoal: BodyGoal = _uiState.value.bodyGoal
    ) {
        val current = _uiState.value
        val plan = current.profile?.let { nutritionEngine.compute(it, activityLevel, bodyGoal) }
        _uiState.value = current.copy(activityLevel = activityLevel, bodyGoal = bodyGoal, plan = plan)
    }
}
