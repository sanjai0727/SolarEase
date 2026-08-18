"""create_otp_verifications_table

Revision ID: 2345678901bc
Revises: 1234567890ab
Create Date: 2026-08-04 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '2345678901bc'
down_revision: Union[str, None] = '1234567890ab'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table('otp_verifications',
    sa.Column('id', sa.UUID(as_uuid=True), nullable=False),
    sa.Column('mobile_number', sa.String(length=10), nullable=False),
    sa.Column('otp_hash', sa.String(), nullable=False),
    sa.Column('expires_at', sa.DateTime(timezone=True), nullable=False),
    sa.Column('attempt_count', sa.Integer(), nullable=False),
    sa.Column('verified', sa.Boolean(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_otp_verifications_mobile_number'), 'otp_verifications', ['mobile_number'], unique=False)


def downgrade() -> None:
    op.drop_index(op.f('ix_otp_verifications_mobile_number'), table_name='otp_verifications')
    op.drop_table('otp_verifications')
