#!/bin/bash

# ==============================================================================
# Global Class Offering Booking System — Concurrency Safety Test Suite
# ==============================================================================
# This script performs two distinct concurrent load tests to verify our
# transactional safety and pessimistic locking strategy:
#
# PART A: Multiple parents booking the same offering simultaneously.
#         -> Verifies Offering Capacity inventory locking (preventing over-enrollment).
#
# PART B: A single parent attempting overlapping bookings concurrently.
#         -> Verifies Parent-level Schedule Conflict locking (preventing double-booking).
# ==============================================================================

set -e

BASE_URL="http://localhost:8080"
UNIQUE_ID=$(date +%s)

# Helper function to check if server is running
check_server() {
  if ! curl -s -o /dev/null "$BASE_URL/api-docs"; then
    echo "❌ Error: Spring Boot server is not running at $BASE_URL."
    echo "   Please start the application with './mvnw spring-boot:run' first."
    exit 1
  fi
}

check_server

# Setup Course & Teacher templates
echo "======================================================================"
echo "🚀 Initializing Concurrency Test Environment (ID: $UNIQUE_ID)"
echo "======================================================================"

# 1. Create Course Templates
echo "📝 Step 1: Registering Course templates..."
COURSE_RESP_A=$(curl -s -X POST "$BASE_URL/api/courses" \
  -H "Content-Type: application/json" \
  -d "{\"title\": \"Concurrency Course A $UNIQUE_ID\", \"description\": \"Test description A\"}")
COURSE_ID_A=$(echo "$COURSE_RESP_A" | jq -r '.id')

COURSE_RESP_B=$(curl -s -X POST "$BASE_URL/api/courses" \
  -H "Content-Type: application/json" \
  -d "{\"title\": \"Concurrency Course B $UNIQUE_ID\", \"description\": \"Test description B\"}")
COURSE_ID_B=$(echo "$COURSE_RESP_B" | jq -r '.id')

echo "   Course A: $COURSE_ID_A"
echo "   Course B: $COURSE_ID_B"

# 2. Create Teacher
echo "📝 Step 2: Registering Teacher profile..."
TEACHER_RESP=$(curl -s -X POST "$BASE_URL/api/teachers" \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"Teacher $UNIQUE_ID\", \"email\": \"teacher.$UNIQUE_ID@undoschool.com\", \"timezone\": \"UTC\"}")
TEACHER_ID=$(echo "$TEACHER_RESP" | jq -r '.id')
echo "   Teacher: $TEACHER_ID"

echo ""
echo "======================================================================"
echo "🔥 TEST CASE A: Multiple Parents Booking Same Offering (Capacity Safety)"
echo "======================================================================"
# Scenario: An offering has maxStudents = 2. 10 parents book it at the same time.
# Expected: Exactly 2 succeed (HTTP 201). 8 fail with 409/400 (Class is full).

# 3. Create Offering A (Capacity = 2)
echo "📝 Step A.1: Creating Offering A (maxStudents = 2)..."
OFFERING_RESP_A=$(curl -s -X POST "$BASE_URL/api/teachers/$TEACHER_ID/offerings" \
  -H "Content-Type: application/json" \
  -d "{\"courseId\": \"$COURSE_ID_A\", \"title\": \"Saturday Batch\", \"maxStudents\": 2}")
OFFERING_ID_A=$(echo "$OFFERING_RESP_A" | jq -r '.offeringId')
echo "   Offering A: $OFFERING_ID_A"

# 4. Add sessions to Offering A
echo "📝 Step A.2: Adding sessions to Offering A (12:00-13:00 UTC)..."
curl -s -o /dev/null -X POST "$BASE_URL/api/offerings/$OFFERING_ID_A/sessions" \
  -H "Content-Type: application/json" \
  -d "{\"sessions\": [{\"startTime\": \"2026-06-15T12:00:00Z\", \"endTime\": \"2026-06-15T13:00:00Z\"}]}"
echo "   Sessions registered."

# 5. Create 10 Parents for Test Case A
echo "📝 Step A.3: Creating 10 Parent profiles..."
declare -a PARENT_IDS_A
for i in {1..10}; do
  PARENT_RESP=$(curl -s -X POST "$BASE_URL/api/parents" \
    -H "Content-Type: application/json" \
    -d "{\"name\": \"Parent $i $UNIQUE_ID\", \"email\": \"parent.a.$i.$UNIQUE_ID@undoschool.com\", \"timezone\": \"America/New_York\"}")
  PARENT_IDS_A[$i]=$(echo "$PARENT_RESP" | jq -r '.id')
done
echo "   10 Parents registered."

# 6. Execute simultaneous requests
echo "⚡ Step A.4: Firing 10 concurrent requests to book Offering A..."
echo "----------------------------------------------------------------------"
LOG_A=$(mktemp)
for i in {1..10}; do
  (
    RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/parents/${PARENT_IDS_A[$i]}/bookings" \
      -H "Content-Type: application/json" \
      -d "{\"offeringId\": \"$OFFERING_ID_A\"}")
    HTTP_STATUS=$(echo "$RESP" | tail -n 1)
    echo "Parent $i Booking Response: HTTP $HTTP_STATUS" >> "$LOG_A"
  ) &
done
wait
cat "$LOG_A"
rm -f "$LOG_A"
echo "----------------------------------------------------------------------"

# 7. Check final enrollment state
FINAL_OFFERINGS_A=$(curl -s -X GET "$BASE_URL/api/parents/offerings")
ENROLLMENT_A=$(echo "$FINAL_OFFERINGS_A" | jq -r ".[] | select(.offeringId == \"$OFFERING_ID_A\") | .currentEnrollment")
echo "📊 Results:"
echo "   Offering Capacity Limit: 2"
echo "   Final Enrollment Count:  $ENROLLMENT_A"

if [ "$ENROLLMENT_A" -eq 2 ]; then
  echo "🎉 SUCCESS: Pessimistic locking successfully protected seat inventory!"
else
  echo "❌ FAILURE: Over-booking occurred. Enrollment: $ENROLLMENT_A"
fi

echo ""
echo "======================================================================"
echo "🔥 TEST CASE B: One Parent booking overlapping offerings (Parent Locking)"
echo "======================================================================"
# Scenario: Create Offering X (12:00-13:00) and Offering Y (12:30-13:30) which overlap.
# Parent tries to book both simultaneously.
# Expected: Exactly 1 succeeds (HTTP 201) and 1 fails with 409 Conflict.

# 1. Create Offering X (12:00 - 13:00 UTC)
echo "📝 Step B.1: Creating Offering X (12:00-13:00 UTC)..."
OFFERING_RESP_X=$(curl -s -X POST "$BASE_URL/api/teachers/$TEACHER_ID/offerings" \
  -H "Content-Type: application/json" \
  -d "{\"courseId\": \"$COURSE_ID_A\", \"title\": \"Section X\", \"maxStudents\": 10}")
OFFERING_ID_X=$(echo "$OFFERING_RESP_X" | jq -r '.offeringId')
curl -s -o /dev/null -X POST "$BASE_URL/api/offerings/$OFFERING_ID_X/sessions" \
  -H "Content-Type: application/json" \
  -d "{\"sessions\": [{\"startTime\": \"2026-06-15T12:00:00Z\", \"endTime\": \"2026-06-15T13:00:00Z\"}]}"

# 2. Create Offering Y (12:30 - 13:30 UTC) - Overlaps with Offering X
echo "📝 Step B.2: Creating Offering Y (12:30-13:30 UTC - Overlapping)..."
OFFERING_RESP_Y=$(curl -s -X POST "$BASE_URL/api/teachers/$TEACHER_ID/offerings" \
  -H "Content-Type: application/json" \
  -d "{\"courseId\": \"$COURSE_ID_B\", \"title\": \"Section Y\", \"maxStudents\": 10}")
OFFERING_ID_Y=$(echo "$OFFERING_RESP_Y" | jq -r '.offeringId')
curl -s -o /dev/null -X POST "$BASE_URL/api/offerings/$OFFERING_ID_Y/sessions" \
  -H "Content-Type: application/json" \
  -d "{\"sessions\": [{\"startTime\": \"2026-06-15T12:30:00Z\", \"endTime\": \"2026-06-15T13:30:00Z\"}]}"

# 3. Create a single Parent profile
echo "📝 Step B.3: Creating single Parent profile..."
PARENT_RESP_B=$(curl -s -X POST "$BASE_URL/api/parents" \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"Simultaneous Parent $UNIQUE_ID\", \"email\": \"parent.b.$UNIQUE_ID@undoschool.com\", \"timezone\": \"UTC\"}")
PARENT_ID_B=$(echo "$PARENT_RESP_B" | jq -r '.id')
echo "   Parent ID: $PARENT_ID_B"

# 4. Fire overlapping bookings in parallel for the same parent
echo "⚡ Step B.4: Firing parallel booking requests for Offering X & Offering Y..."
echo "----------------------------------------------------------------------"
LOG_B=$(mktemp)

# Try booking Offering X
(
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/parents/$PARENT_ID_B/bookings" \
    -H "Content-Type: application/json" \
    -d "{\"offeringId\": \"$OFFERING_ID_X\"}")
  HTTP_STATUS=$(echo "$RESP" | tail -n 1)
  echo "Offering X Booking Response: HTTP $HTTP_STATUS" >> "$LOG_B"
) &

# Try booking Offering Y (simultaneously)
(
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/parents/$PARENT_ID_B/bookings" \
    -H "Content-Type: application/json" \
    -d "{\"offeringId\": \"$OFFERING_ID_Y\"}")
  HTTP_STATUS=$(echo "$RESP" | tail -n 1)
  echo "Offering Y Booking Response: HTTP $HTTP_STATUS" >> "$LOG_B"
) &

wait
cat "$LOG_B"
rm -f "$LOG_B"
echo "----------------------------------------------------------------------"

# 5. Verify Parent Bookings
echo "✅ Step B.5: Verifying parent's final bookings in database..."
FINAL_BOOKINGS=$(curl -s -X GET "$BASE_URL/api/parents/$PARENT_ID_B/bookings")
BOOKING_COUNT=$(echo "$FINAL_BOOKINGS" | jq '. | length')

echo "📊 Results:"
echo "   Simultaneous booking attempts: 2 (X and Y overlap)"
echo "   Final booked offerings count: $BOOKING_COUNT"

if [ "$BOOKING_COUNT" -eq 1 ]; then
  echo "🎉 SUCCESS: Parent-level locking prevented overlapping double-bookings!"
else
  echo "❌ FAILURE: Parent double-booked overlapping offerings! Bookings count: $BOOKING_COUNT"
fi
echo "======================================================================"
