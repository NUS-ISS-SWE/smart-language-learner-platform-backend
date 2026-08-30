from app.event_time import composition_service_timestamp


def test_composition_service_timestamp_has_no_utc_offset_for_java_local_date_time():
    timestamp = composition_service_timestamp()

    assert "+" not in timestamp
    assert not timestamp.endswith("Z")
