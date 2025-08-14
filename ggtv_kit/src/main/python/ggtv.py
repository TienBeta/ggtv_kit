"""
Android TV Remote Python Module
"""
import nest_asyncio
nest_asyncio.apply()
import asyncio
import logging
import os
from os.path import join
from typing import Optional
from androidtvremote2 import (
    AndroidTVRemote,
    CannotConnect,
    ConnectionClosed,
    InvalidAuth,
    VolumeInfo,
)
import time
import threading
from threading import Lock
import concurrent.futures

# Custom exceptions to replace string matching
class PairingRequiredException(Exception):
    """Exception raised when pairing is required"""
    pass

class ConnectionFailedException(Exception):
    """Exception raised when connection fails"""
    pass

# Thread safety with locks
_remote_lock = Lock()
_time_lock = Lock()

# Thread pool to manage threads
_thread_pool = concurrent.futures.ThreadPoolExecutor(max_workers=5)

# Global variables to maintain connection
remote_instance: Optional[AndroidTVRemote] = None

# Track last command time with thread safety
last_command_time = None
ping_interval = 5  # Ping every 5s to avoid timeout 20s

# Key mapping
KEY_MAPPING = {
    "KEYCODE_POWER": "POWER",
    "KEYCODE_HOME": "HOME",
    "KEYCODE_BACK": "BACK",
    "KEYCODE_VOLUME_UP": "VOLUME_UP",
    "KEYCODE_VOLUME_DOWN": "VOLUME_DOWN",
    "KEYCODE_DPAD_CENTER": "DPAD_CENTER",
    "KEYCODE_DPAD_UP": "DPAD_UP",
    "KEYCODE_DPAD_DOWN": "DPAD_DOWN",
    "KEYCODE_DPAD_LEFT": "DPAD_LEFT",
    "KEYCODE_DPAD_RIGHT": "DPAD_RIGHT",
    "KEYCODE_MENU": "MENU",
    "KEYCODE_MUTE": "MUTE",
}

def setup_logging():
    """Setup logging for debug"""
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)
    return logger

logger = setup_logging()

def create_cert_files():
    """
    Create certificate files for AndroidTVRemote
    cert.pem & key.pem
    """
    try:
        # create files
        cert_file = join(os.environ["HOME"], "cert.pem")
        key_file = join(os.environ["HOME"], "key.pem")

        return cert_file, key_file
    except Exception as e:
        logger.error(f"cert files failed: {e}")
        return None, None

def should_reconnect() -> bool:
    """
    Check if need reconnect to TV
    Fixed: Thread safety with lock
    """
    with _time_lock:
        if not last_command_time:
            return False

        now = time.time()
        time_since_last = now - last_command_time

    logger.info(f"should_reconnect: time_since_last={time_since_last:.1f}s, ping_interval={ping_interval}s")
    return time_since_last >= ping_interval

def try_reconnect() -> bool:
    """
    Send reconnect to TV
    Fixed: Better error handling
    """
    global remote_instance

    with _remote_lock:
        if not remote_instance:
            return False

        try:
            logger.info("Attempting reconnection...")
            return run_async_from_sync(async_retry_connection())
        except Exception as e:
            logger.warning(f"Reconnection failed: {e}")
            return False

async def async_pair(remote: AndroidTVRemote) -> str:
    """
    Pairing with Android TV - return pairing code if needed
    """
    try:
        name, mac = await remote.async_get_name_and_mac()
        logger.info(f"pairing with {remote.host} {name} {mac}")

        await remote.async_start_pairing()
        logger.info("pairing started. check on TV to get pairing code.")

        # Return signal to app handle send pairing code
        return "PAIRING_REQUIRED"

    except Exception as e:
        logger.error(f"pairing failed: {e}")
        return "PAIRING_ERROR"

async def async_finish_pairing(remote: AndroidTVRemote, pairing_code: str) -> bool:
    """
    Finish pairing with code from TV
    """
    try:
        logger.info(f"finish pairing with code: {pairing_code}")
        await remote.async_finish_pairing(pairing_code)
        logger.info("Pairing successful!")
        return True

    except InvalidAuth:
        logger.warning("Pairing code invalid!")
        return False
    except ConnectionClosed:
        logger.warning("Connection closed, please try again")
        return False
    except Exception as e:
        logger.error(f"finish pairing failed: {e}")
        return False

async def async_connect_to_tv(host: str, appName: str) -> bool:
    """
    Async connect to Android TV
    Fixed: Better exception handling with custom exceptions
    """
    global remote_instance

    try:
        logger.info(f"Connecting {host}")

        # Create certificate files
        cert_file, key_file = create_cert_files()
        if not cert_file or not key_file:
            return False

        # Create instance of AndroidTVRemote
        with _remote_lock:
            remote_instance = AndroidTVRemote(
                appName,  # client_name
                cert_file,             # certfile
                key_file,              # keyfile
                host                   # host
            )

        # Generate certificate if needed
        if await remote_instance.async_generate_cert_if_missing():
            logger.info("create new certificate succeed, need pairing")
            pairing_result = await async_pair(remote_instance)
            if pairing_result == "PAIRING_REQUIRED":
                # Raise custom exception
                raise PairingRequiredException(pairing_result)
            elif pairing_result == "PAIRING_ERROR":
                return False

        # Try to connect
        max_retries = 3
        for attempt in range(max_retries):
            try:
                await remote_instance.async_connect()
                break
            except InvalidAuth:
                logger.warning(f"Need pair again, attempt {attempt + 1}")
                pairing_result = await async_pair(remote_instance)
                if pairing_result == "PAIRING_REQUIRED":
                    raise PairingRequiredException(pairing_result)
                continue
            except (CannotConnect, ConnectionClosed) as e:
                logger.error(f"Cannot connect: {e}")
                if attempt == max_retries - 1:
                    return False
                await asyncio.sleep(2)

        # Keep reconnecting
        remote_instance.keep_reconnecting()

        logger.info("Connect successful!")
        logger.info(f"Device info: {remote_instance.device_info}")
        logger.info(f"Is on: {remote_instance.is_on}")
        logger.info(f"Current app: {remote_instance.current_app}")

        return True

    except PairingRequiredException as e:
        # Re-raise for Kotlin to handle
        raise e
    except Exception as e:
        logger.error(f"Connect error: {e}")
        return False

async def async_disconnect_from_tv():
    """
    Disconnect from Android TV
    """
    global remote_instance

    try:
        with _remote_lock:
            if remote_instance:
                remote_instance.disconnect()
                remote_instance = None
                logger.info("Disconnected!")
    except Exception as e:
        logger.error(f"Disconnect error: {e}")

async def async_send_key(key_code: str) -> bool:
    """
    Send key command to Android TV
    Fixed: Better reconnection logic and error handling
    """
    global remote_instance, last_command_time

    with _remote_lock:
        if not remote_instance:
            raise Exception("Not connected to TV yet")

    # Ping check if needed before send command
    if should_reconnect():
        if not try_reconnect():
            logger.error("Reconnection failed, cannot send command")
            raise ConnectionFailedException("Failed to reconnect to TV")

    max_retries = 3
    for attempt in range(max_retries):
        try:
            if key_code in KEY_MAPPING:
                command = KEY_MAPPING[key_code]
            else:
                # Try to send directly
                command = key_code.replace("KEYCODE_", "")

            with _remote_lock:
                remote_instance.send_key_command(command)

            logger.info(f"key sent: {key_code} -> {command}")

            # Update last command time with thread safety
            update_last_activity()
            return True

        except Exception as e:
            logger.error(f"send key error: {e}")

            if attempt < max_retries - 1:  # retry still remain
                try:
                    # Quick reconnecting
                    logger.info("Attempting quick reconnect...")
                    await asyncio.wait_for(remote_instance.async_connect(),5)
                    remote_instance.keep_reconnecting()
                    time.sleep(1)  # Short waiting
                    with _remote_lock:
                        remote_instance.send_key_command(command)
                    logger.info(f"key sent: {key_code} -> {command} after reconnected")
                    update_last_activity()
                    return True
                except Exception as reconnect_error:
                    logger.error(f"Quick reconnect failed: {reconnect_error}")
                    if attempt == max_retries - 1:
                        raise ConnectionFailedException("Failed to reconnect to TV")
            else:
                logger.error(f"Send key error after {max_retries} attempts: {e}")
                raise Exception("Failed to send key to TV")

def update_last_activity():
    """
    Update last time command with thread safety
    """
    global last_command_time
    with _time_lock:
        last_command_time = time.time()

def run_async_from_sync(coro, timeout=5):
    """
    run async from sync with timeout.
    """
    try:
        # Sử dụng asyncio.wait_for để đảm bảo timeout luôn được áp dụng
        timed_coro = asyncio.wait_for(coro, timeout=timeout)
        return asyncio.run(timed_coro)
    except asyncio.TimeoutError:
        logger.error(f"Tác vụ đã hết thời gian chờ sau {timeout} giây.")
        raise  # Ném lại lỗi TimeoutError để code gọi nó có thể xử lý

def finish_pairing(pairing_code: str) -> bool:
    """
    Wrapper function for Kotlin to complete pairing
    """
    global remote_instance

    with _remote_lock:
        if not remote_instance:
            logger.error("Doesn't have remote instance for pairing")
            return False

    try:
        return run_async_from_sync(async_finish_pairing(remote_instance, pairing_code))
    except Exception as e:
        logger.error(f"finish_pairing error: {e}")
        return False

def retry_connection() -> bool:
    """
    Wrapper function to retry connect after pairing succeed
    """
    global remote_instance

    with _remote_lock:
        if not remote_instance:
            logger.error("Doesn't have remote instance to connect")
            return False

    try:
        return run_async_from_sync(async_retry_connection())
    except Exception as e:
        logger.error(f"retry_connection error: {e}")
        return False

async def async_retry_connection() -> bool:
    """
    Async retry connect after pairing succeed
    """
    global remote_instance

    try:
        await remote_instance.async_connect()
        remote_instance.keep_reconnecting()

        logger.info("Connect successful!")
        logger.info(f"Device info: {remote_instance.device_info}")
        logger.info(f"Is on: {remote_instance.is_on}")

        return True

    except Exception as e:
        logger.error(f"async_retry_connection error: {e}")
        return False

def connect_to_tv(host: str, appName: str) -> bool:
    """
    Wrapper function for Kotlin to connect to TV
    Fixed: Better exception handling
    """
    try:
        return run_async_from_sync(async_connect_to_tv(host, appName))
    except PairingRequiredException as e:
        # Re-raise for Kotlin to know pairing is needed
        raise e
    except Exception as e:
        logger.error(f"connect_to_tv error: {e}")
        return False

def disconnect_from_tv():
    """
    Wrapper function for Kotlin to disconnect from TV
    """
    try:
        run_async_from_sync(async_disconnect_from_tv())
    except Exception as e:
        logger.error(f"disconnect_from_tv error: {e}")

def send_key(key_code: str) -> bool:
    """
    Wrapper function for Kotlin to send key
    """
    try:
        return run_async_from_sync(async_send_key(key_code))
    except Exception as e:
        logger.error(f"send_key error: {e}")
        raise e

def send_app_link(url: str):
    """
    Send app link to open app, for example: https://www.youtube.com or youtube://
    Fixed: Thread safety
    """
    global remote_instance

    with _remote_lock:
        if not remote_instance:
            raise Exception("Not connected to TV yet!")

    try:
        with _remote_lock:
            remote_instance.send_launch_app_command(url)
        update_last_activity()
        logger.info(f"app link sent: {url}")
    except Exception as e:
        logger.error(f"send_app_link error: {e}")
        raise e

def send_text(text: str):
    """
    Send text to TV
    Fixed: Thread safety
    """
    global remote_instance

    with _remote_lock:
        if not remote_instance:
            raise Exception("Not connected to TV yet!")

    try:
        with _remote_lock:
            remote_instance.send_text(text)
        update_last_activity()
        logger.info(f"text sent: {text}")
    except Exception as e:
        logger.error(f"send_text error: {e}")
        raise e

# Default app constants
COMMON_APPS = {
     "appletv": "https://tv.apple.com",
     "youtube": "https://www.youtube.com",
     "netflix": "com.netflix.ninja",
     "disney+": "com.disney.disneyplus",
     "amazon prime": "com.amazon.amazonvideo.livingroom",
     "kodi": "org.xbmc.kodi",
     "hulu": "com.hulu.livingroomplus",
     "max": "com.maxtvplus.yorchapps",
     "spotify": "com.spotify.tv.android",
     "pluto": "tv.pluto.android",
}

def open_app(app_name: str):
    """
    Open app by name, for example: youtube
    """
    app_name_lower = app_name.lower().strip()
    if app_name_lower in COMMON_APPS:
        send_app_link(COMMON_APPS[app_name_lower])
    else:
        logger.warning(f"App link not found: {app_name}")

def get_device_info() -> dict:
    """
    Get device info
    Fixed: Thread safety
    """
    global remote_instance

    with _remote_lock:
        if remote_instance:
            return {
                "device_info": str(remote_instance.device_info),
                "is_on": remote_instance.is_on,
                "current_app": remote_instance.current_app,
                "volume_info": str(remote_instance.volume_info) if hasattr(remote_instance, 'volume_info') else None
            }
    return {}

# Cleanup function
def cleanup():
    """
    Cleanup when app closes
    Fixed: Also cleanup thread pool and proper thread safety
    """
    global remote_instance, _thread_pool

    with _remote_lock:
        if remote_instance:
            try:
                remote_instance.disconnect()
            except:
                pass
            remote_instance = None

    # Cleanup thread pool
    if _thread_pool:
        _thread_pool.shutdown(wait=True)