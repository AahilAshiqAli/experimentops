# Purpose: Write customer-visible and downloadable logs for an active experiment run.

from __future__ import annotations

from experiment_runtime.models import ExperimentExecutionContext


def run_info(
    context: ExperimentExecutionContext,
    message: str,
    *args: object,
) -> None:
    """Write an INFO record that is also forwarded with the next progress event."""

    if context.run_log_sink is not None:
        context.run_log_sink.info(context.experiment_type, message, *args)


def run_verbose(
    context: ExperimentExecutionContext,
    message: str,
    *args: object,
) -> None:
    """Write detailed diagnostics to the downloadable run log."""

    if context.run_log_sink is not None:
        context.run_log_sink.verbose(context.experiment_type, message, *args)
