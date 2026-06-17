# -*- coding: utf-8 -*-
"""WebSocket connection used to interrupt the agent's replying in AgentScope
Studio"""
import socketio
from agentscope.agent import ReActAgent

from hook import studio_post_reply_hook


class StudioConnect:
    """The connection to the studio."""

    _friday_namespace = "/friday"

    def __init__(self, url: str, agent: ReActAgent):
        """Initialize the connection with the studio URL."""
        self.url = url
        self.sio = socketio.AsyncClient(
            reconnection=True,
            reconnection_attempts=5,
            reconnection_delay=5,
            reconnection_delay_max=3,
        )

        self.agent = agent

        @self.sio.on("connect", namespace=self._friday_namespace)
        async def on_connect():
            print(f"Connect to the studio at {self.url} successfully.")

        @self.sio.on("disconnect", namespace=self._friday_namespace)
        async def on_disconnect():
            print(f"Disconnected from the studio at {self.url}.")

        @self.sio.on("interrupt", namespace=self._friday_namespace)
        async def on_interrupt():
            print("Interrupt received, stopping the agent...")
            await self.agent.interrupt()

            # Finish the reply and notify the studio
            studio_post_reply_hook(self.agent)

    async def connect(self) -> None:
        try:
            await self.sio.connect(
                f"{self.url}",
                namespaces=["/friday"],
            )
        except Exception as e:
            raise RuntimeError(
                f"Failed to connect to the studio at {self.url}. "
                "Please check if the studio is running and the URL is correct."
            ) from e

    async def disconnect(self) -> None:
        try:
            print("Disconnecting from the studio...")
            await self.sio.disconnect()
            print("Successfully disconnected.")
        except Exception as e:
            raise RuntimeError(
                f"Failed to disconnect from the studio at {self.url}."
            ) from e