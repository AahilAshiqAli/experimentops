# Purpose: Define the common error raised when an execution violates its experiment contract.


class ExperimentContractError(ValueError):
    """Raised when an execution context does not satisfy an experiment contract."""
