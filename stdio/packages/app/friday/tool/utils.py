# -*- coding: utf-8 -*-
"""The agentscope retrieval tool module."""
import agentscope
import requests
from agentscope.message import TextBlock
from agentscope.tool import ToolResponse

from tool.agentscope_tools import get_agentscope_module_signatures


def view_agentscope_readme() -> ToolResponse:
    """View README.md in AgentScope repository, which contains brief introduction, quickstart, and several examples of AgentScope library."""
    # Download the README.md from the GitHub file https://github.com/agentscope-ai/agentscope/blob/main/README.md
    if not hasattr(view_agentscope_readme, "readme"):
        try:
            response = requests.get(
                "https://raw.githubusercontent.com/agentscope-ai/agentscope/main/README.md"
            )
            response.raise_for_status()
            view_agentscope_readme.readme = response.text
        except Exception:
            return ToolResponse(
                content=[
                    TextBlock(
                        type="text",
                        text="Failed to download README.md from AgentScope repository. You can try to visit the repository directly at https://github.com/agentscope-ai/agentscope"
                    )
                ]
            )

    return ToolResponse(
        content=[
            TextBlock(
                type='text',
                text=view_agentscope_readme.readme
            )
        ]
    )

def view_agentscope_faq() -> ToolResponse:
    """View AgentScope's FAQ file, which contains frequently asked questions and their answers about AgentScope library."""
    if not hasattr(view_agentscope_faq, "faq"):
        # Download from https://github.com/agentscope-ai/agentscope/blob/main/docs/tutorial/en/src/faq.py
        try:
            response = requests.get(
                "https://raw.githubusercontent.com/agentscope-ai/agentscope/main/docs/tutorial/en/src/faq.py"
            )
            response.raise_for_status()
            view_agentscope_faq.faq = response.text
        except Exception:
            return ToolResponse(
                content=[
                    TextBlock(
                        type="text",
                        text="Failed to download FAQ from AgentScope repository. You can try to visit the FAQ directly at https://doc.agentscope.io/tutorial/faq.html"
                    )
                ]
            )

    return ToolResponse(
        content=[
            TextBlock(
                type='text',
                text=view_agentscope_faq.faq
            )
        ]
    )

def view_agentscope_library(
    module: str = "agentscope"
) -> ToolResponse:
    """View AgentScope's Python library by given a module name (e.g. agentscope), and return the module's submodules, classes, and functions. Given a class name, return the class's documentation, methods, and their signatures. Given a function name, return the function's documentation and signature. If you don't have any information about AgentScope library, try to use "agentscope" to view the available top modules.

    Note this function only provide the module's brief information. For more information, you should view the source code.

    Args:
        module (`str`):
            The module name to view, which should be a module path separated by dots (e.g. "agentscope.models"). It can refer to a module, a class, or a function.
    """
    if not module.startswith("agentscope"):
        return ToolResponse(
            content=[
                TextBlock(
                    type='text',
                    text=f"Module '{module}' is invalid. The input module should "
                        f"be 'agentscope' or submodule of 'agentscope.xxx.xxx' "
                        f"(separated by dots)."
                )
            ]
        )

    agentscope_top_modules = {}
    for as_module in agentscope.__all__:
        if as_module in ["__version__", "logger"]:
            continue
        agentscope_top_modules[as_module] = getattr(agentscope, as_module).__doc__

    # top modules
    if module == "agentscope":
        top_modules_description = [
            "The top-level modules in AgentScope library:"
        ] + [
            f"- agentscope.{k}: {v}" for k, v in agentscope_top_modules.items()
        ] + ["You can further view the classes/function within above modules by calling this function with the above module name."]
        return ToolResponse(
            content=[
                TextBlock(
                    type="text",
                    text="\n".join(top_modules_description)
                )
            ]
        )

    # class, functions
    modules = get_agentscope_module_signatures()
    for as_module in modules:
        if as_module.module == module:
            return ToolResponse(
                content=[
                    TextBlock(
                        type="text",
                        text=f"""- The signature of '{module}':
```python
{as_module.signature}
```
- Source code reference: {as_module.reference}"""
                    )
                ]
            )

    # two-level modules
    collected_modules = []
    for as_module in modules:
        if as_module.module.startswith(module):
            collected_modules.append(as_module)

    if len(collected_modules) > 0:
        collected_modules_content = [
            f"The classes/functions and their truncated docstring in '{module}' module:"
        ] + [f"- {_.module}: {repr(_.docstring)}" for _ in collected_modules] + [
            "The docstring is truncated for limited context. For detailed signature and methods, call this function with the above module name"
        ]

        return ToolResponse(
            content=[
                TextBlock(
                    type="text",
                    text="\n".join(collected_modules_content)
                )
            ]
        )

    return ToolResponse(
        content=[
            TextBlock(
                type="text",
                text=f"Module '{module}' not found. Use 'agentscope' to view the "
                f"top-level modules to ensure the given module is valid."
            )
        ]
    )
