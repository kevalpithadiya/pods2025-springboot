import sys
import random
from threading import Thread
import requests

from user import post_user
from wallet import put_wallet, get_wallet
from marketplace import (
    post_order,
    get_product,
    test_get_product_stock,
    test_post_order
)
from utils import check_response_status_code, print_fail_message, print_pass_message

MARKETPLACE_SERVICE_URL = "http://localhost:8081"

def place_order_thread(tid, user_id, product_id, attempts=5):
    """
    Each thread attempts to place 'attempts' orders for the same product_id, quantity=1.
    If an order is successfully placed (HTTP 201), increment global successful_orders.
    We also call test_post_order(...) to verify the response structure.
    """
    global order_prices
    for _ in range(attempts):
        resp = post_order(user_id, [{"product_id": product_id, "quantity": 1}])

        if resp.status_code == 201:
            order_prices[tid] = resp.json()["total_price"]
        elif resp.status_code == 400:
            print_fail_message("Unexpected order placement failure")
        else:
            print_fail_message(f"Unexpected status code {resp.status_code} for POST /orders.")

# To test if the same user sending the first 5 orders concurrently
# results in both being discounted.

def main():
    try:
        # 2) Create user (large enough balance so they can buy many items)
        user_id = 2001
        resp = post_user(user_id, "Bob Market", "bob@market.com")
        if not check_response_status_code(resp, 201):
            return False

        # 3) Credit wallet significantly (e.g. 200000)
        resp = put_wallet(user_id, "credit", 2000000000)
        if not check_response_status_code(resp, 200):
            return False

        # 4) Get the product price and available stock
        product_id = 101
        resp = get_product(product_id)
        assert resp.status_code == 200
        product = resp.json()
        price, stock = product["price"], product["stock_quantity"]

        # 5) Launch concurrency threads that place orders for product_id=101, quantity=1
        global order_prices
        order_prices = [0 for _ in range(stock)]

        # Try to place all orders concurrently
        thread_count = stock
        attempts_per_thread = 1
        threads = []

        for i in range(thread_count):
            t = Thread(target=place_order_thread, kwargs={
                "tid": i,
                "user_id": user_id,
                "product_id": product_id,
                "attempts": attempts_per_thread
            })
            threads.append(t)
            t.start()

        for t in threads:
            t.join()

        discounted_orders = 0
        for i in range(thread_count):
            if (0 < order_prices[i] < price):
                discounted_orders += 1
        
        if (discounted_orders == 0):
            print_fail_message("User recieved 0 discounts")
        elif (discounted_orders == 1):
            print_pass_message("User only recieved 1 discount")
        else:
            print_fail_message(f"User recieved {discounted_orders} discounts")
            print("Could not find a reliable fix for this issue since multiple services are involved")

    except Exception as e:
        print_fail_message(f"Test crashed: {e}")
        return False

if __name__ == "__main__":
    if main():
        sys.exit(0)
    else:
        sys.exit(1)
