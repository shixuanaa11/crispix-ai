# -*- coding: utf-8 -*-
"""Get the signatures of functions and classes in the agentscope library."""
import inspect
from typing import Literal

import agentscope
from pydantic import BaseModel


def get_class_signature(cls) -> str:
    """Get the signature of a class.

    Args:
        cls: A class object.

    Returns:
        str: The signature of the class.
    """
    # Obtain class name and docstring
    class_name = cls.__name__
    class_docstring = cls.__doc__ or ""

    # Construct the class string
    class_str = f"class {class_name}:\n"
    if class_docstring:
        class_str += f'    """{class_docstring}"""\n'

    # Obtain the module of the class
    methods = []
    for name, method in inspect.getmembers(cls, predicate=inspect.isfunction):
        # Skip methods that are not part of the class
        if method.__qualname__.split('.')[0] != class_name:
            continue

        if name.startswith("_") and name not in ["__init__", "__call__"]:
            continue

        # Obtain the method's signature
        sig = inspect.signature(method)

        # Construct the method string
        method_str = f"    def {name}{sig}:\n"

        # Add the method's docstring if it exists
        method_docstring = method.__doc__ or ""
        if method_docstring:
            method_str += f'        """{method_docstring}"""\n'

        methods.append(method_str)

    class_str += "\n".join(methods)
    return class_str

def get_function_signature(func) -> str:
    sig = inspect.signature(func)
    method_str = f"def {func.__name__}{sig}:\n"

    method_docstring = func.__doc__ or ""
    if method_docstring:
        method_str += f'   """{method_docstring}"""\n'

    return method_str


class FuncOrCls(BaseModel):
    """The class records the module, signature, docstring, reference, and
    type"""
    module: str
    """The module of the function or class."""
    signature: str
    """The signature of the function or class."""
    docstring: str
    """The docstring of the function or class."""
    reference: str
    """The reference to the source code of the function or class"""
    type: Literal["function", "class"]
    """The type of the function or class, either 'function' or 'class'."""

    def __init__(
        self,
        module: str,
        signature: str,
        docstring: str,
        reference: str,
        type: Literal["function", "class"]
    ) -> None:
        """Initialize the FuncOrCls instance."""
        super().__init__(
            module=module,
            signature=signature.strip(),
            docstring=docstring.strip(),
            reference=reference,
            type=type
        )

def _truncate_docstring(docstring: str, max_length: int = 200) -> str:
    """Truncate the docstring to a maximum length."""
    if len(docstring) > max_length:
        return docstring[:max_length] + "..."
    return docstring

def get_agentscope_module_signatures() -> list[FuncOrCls]:
    """Get the signatures of functions and classes in the agentscope library.
    """
    signatures = []
    for module in agentscope.__all__:
        as_module = getattr(agentscope, module)
        path_module = ".".join(["agentscope", module])

        # Functions
        if inspect.isfunction(as_module):
            file = inspect.getfile(as_module)
            source_lines, start_line = inspect.getsourcelines(as_module)
            signatures.append(
                FuncOrCls(
                    module=path_module,
                    signature=get_function_signature(as_module),
                    docstring=_truncate_docstring(as_module.__doc__ or ""),
                    reference=f"{file}: {start_line}-{start_line + len(source_lines)}",
                    type="function",
                )
            )

        else:
            if not hasattr(as_module, "__all__"):
                continue

            # Modules with __all__ attribute
            for name in as_module.__all__:
                func_or_cls = getattr(as_module, name)
                path_func_or_cls = ".".join([path_module, name])

                if inspect.isclass(func_or_cls):
                    file = inspect.getfile(func_or_cls)
                    source_lines, start_line = inspect.getsourcelines(func_or_cls)
                    signatures.append(
                        FuncOrCls(
                            module=path_func_or_cls,
                            signature=get_class_signature(func_or_cls),
                            docstring=_truncate_docstring(func_or_cls.__doc__ or ""),
                            reference=inspect.getfile(func_or_cls),
                            type="class"
                        )
                    )

                elif inspect.isfunction(func_or_cls):
                    file = inspect.getfile(func_or_cls)
                    source_lines, start_line = inspect.getsourcelines(func_or_cls)
                    signatures.append(
                        FuncOrCls(
                            module=path_func_or_cls,
                            signature=get_function_signature(func_or_cls),
                            docstring=_truncate_docstring(func_or_cls.__doc__ or ""),
                            reference=f"{file}: {start_line}-{start_line + len(source_lines)}",
                            type="function"
                        )
                    )

    return signatures
