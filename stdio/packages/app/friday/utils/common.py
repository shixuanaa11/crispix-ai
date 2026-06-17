# -*- coding: utf-8 -*-
"""Utility functions for file path management in AgentScope Studio."""
import platform
import os

from utils.constants import NAME_STUDIO, NAME_APP


def get_local_file_path(filename: str) -> str:
    """Obtain the local file path for a given filename based on the operating system."""
    if platform.system() == "Windows":
        local_path = os.path.join(os.getenv("APPDATA"), NAME_STUDIO)
    elif platform.system() == "Darwin":
        local_path = os.path.join(
            os.getenv("HOME"), "Library", "Application Support", NAME_STUDIO
        )
    elif platform.system() == "Linux":
        local_path = os.path.join(os.getenv("HOME"), ".local", "share", NAME_STUDIO)
    else:
        raise ValueError(
            f"Unsupported operating system: {platform.system()}, expected"
            f" Windows, Darwin, or Linux."
        )

    if not os.path.exists(os.path.join(local_path, NAME_APP)):
        os.makedirs(os.path.join(local_path, NAME_APP), exist_ok=True)

    return os.path.join(local_path, NAME_APP, filename)
