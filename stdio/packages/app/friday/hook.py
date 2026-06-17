# -*- coding: utf-8 -*-
"""The hooks for the agent"""
from typing import Any

import requests
from agentscope.agent import AgentBase


def studio_pre_print_hook(self: AgentBase, kwargs: dict[str, Any]) -> None:
    """Forward the message to the studio application interface."""
    msg = kwargs["msg"]
    message_data = msg.to_dict()

    message_data["content"] = msg.get_content_blocks()

    n_retry = 0
    while True:
        try:
            res = requests.post(
                f"{studio_pre_print_hook.url}/trpc/pushMessageToFridayApp",
                json={
                    "replyId": self._reply_id,
                    "msg": message_data,
                },
            )
            res.raise_for_status()
            break
        except Exception as e:
            if n_retry < 3:
                n_retry += 1
                continue

            raise e from None


def studio_post_reply_hook(self: AgentBase, *args, **kwargs) -> None:
    """Send the finished signal to the studio application interface."""
    n_retry = 0
    while True:
        try:
            res = requests.post(
                f"{studio_pre_print_hook.url}/trpc/pushFinishedSignalToFridayApp",
                json={"replyId": self._reply_id}
            )
            res.raise_for_status()
            break
        except Exception as e:
            if n_retry < 3:
                n_retry += 1
                continue

            raise e from None
