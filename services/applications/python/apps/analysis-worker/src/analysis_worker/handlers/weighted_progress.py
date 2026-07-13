from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class WeightedProgressCalculator:
    """Converts a step-local progress value into global run progress."""

    completed_weight: float
    current_step_weight: float
    total_weight: float

    def calculate(self, local_progress: int | float) -> float:
        if self.total_weight <= 0:
            return _clamp(local_progress)

        global_progress = (
            (
                self.completed_weight
                + (self.current_step_weight * _clamp(local_progress) / 100)
            )
            / self.total_weight
            * 100
        )
        return round(_clamp(global_progress), 2)


def _clamp(value: int | float) -> float:
    return max(0, min(float(value), 100))
